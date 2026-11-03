package com.escalada.jogo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.escalada.serial.Direcao;

/**
 * O astronauta, com esqueleto simples e cinematica inversa (IK) de 2 ossos nos bracos e pernas.
 *
 * Ciclo de escalada (a cada acerto): a mao agarra a agarradeira, que "desce" em relacao ao corpo enquanto
 * o astronauta se puxa para cima; o tronco se inclina para o lado da mao, o quadril sobe e desce, e os pes
 * se alternam (um pe sobe e e reposicionado enquanto o outro empurra). Um pequeno propulsor ajuda no impulso.
 *
 * Estados: repouso -> alcancando (agarradeira acesa) -> puxando (acerto) -> repouso ... -> celebrando (destino).
 */
public final class Astronauta {
    /**
     * Altura (y) em que as solas das botas tocam a plataforma de destino quando o astronauta chega.
     * Vale para o quadril em y = 180 (ASTRO_Y do GameScreen) e ESCALA = 0.75.
     */
    public static final float Y_SOLA = 94.5f;

    /** Tamanho do astronauta na tela (1 = tamanho original do desenho). */
    private static final float ESCALA = 0.75f;

    private static final float DURACAO_PUXADA = 0.55f;
    /** Distancia vertical entre um degrau e o proximo (e quanto o cenario desce a cada puxada). */
    public static final float PASSO_PX = 52f;
    private static final float AFUNDA = PASSO_PX;       // a agarradeira desce junto com a parede
    private static final float ALCANCE_PE_TRAS = 12f;   // quanto o pe de tras acompanha o degrau antes de soltar
    private static final float BRACO_1 = 88f, BRACO_2 = 84f;
    private static final float PERNA_1 = 50f, PERNA_2 = 50f;

    private final float cx;
    private final boolean costas;                       // true = vista de costas (escalando)
    private final float baseY;                          // altura do quadril em repouso

    // Estado da animacao
    private float lean;                                 // inclinacao do tronco (radianos, + = para a esquerda)
    private float puxarT = 1f;
    private boolean puxando;
    private Direcao dirPuxada;
    private float grabX, grabY;
    private final Color corPegada = new Color(1f, 1f, 1f, 1f);
    private int passoPar;
    private boolean celebrando;
    private float celebT;
    private float alturaMundo, mundoInicio, mundoAlvo;  // rolagem da parede (px): o cenario desce enquanto ele sobe
    private float ritmo = 1f;                           // 1 = velocidade normal da puxada
    private final float[] peOff = new float[2];         // deslocamento vertical de cada pe (tela, px)
    private final float[] peComp = new float[2];        // correcao suave quando uma puxada comeca antes da outra acabar
    private final boolean[] peNoAr = new boolean[2];
    private boolean esqRepouso, dirRepouso;         // mao parada agarrando a parede
    private float olhar;                                // direcao da cabeca: -1 esquerda, +1 direita
    private float olharCima;                            // 0..1: cabeca inclinada para cima
    private float impulso;
    private Direcao alvoAtual;
    private float maoEsqX, maoEsqY, maoDirX, maoDirY;

    // Variaveis auxiliares (evitam criar objetos a cada frame)
    private float hipX, hipY, vx, vy, rx, ry;           // quadril e eixos do tronco
    private float px, py;                               // saida de p()
    private float jx, jy, ex, ey;                       // saida de ik(): junta (cotovelo/joelho) e ponta

    public Astronauta(float x, float baseY) {
        this(x, baseY, false);
    }

    /** @param costas true = vista de costas, de frente para a parede (escalando); false = vista de frente. */
    public Astronauta(float x, float baseY, boolean costas) {
        this.cx = x;
        this.baseY = baseY;
        this.costas = costas;
        resetar();
    }

    /** Volta ao estado inicial (usar ao comecar uma fase). */
    public void resetar() {
        lean = 0f;
        puxarT = 1f;
        puxando = false;
        celebrando = false;
        celebT = 0f;
        impulso = 0f;
        passoPar = 0;
        alvoAtual = null;
        olhar = 0f;
        olharCima = 0f;
        alturaMundo = 0f;
        mundoInicio = 0f;
        mundoAlvo = 0f;
        ritmo = 1f;
        for (int i = 0; i < 2; i++) {
            peOff[i] = 0f;
            peComp[i] = 0f;
            peNoAr[i] = false;
        }
        esqRepouso = true;
        dirRepouso = true;
        // De costas as maos ficam agarradas na parede, na altura da cabeca; de frente, penduradas ao lado do corpo.
        maoEsqX = cx - (costas ? 46f : 58f);
        maoEsqY = baseY + (costas ? 124f : 30f);
        maoDirX = cx + (costas ? 46f : 58f);
        maoDirY = baseY + (costas ? 124f : 30f);
    }

    /** Inicia a puxada: a mao agarra (hx, hy) e o astronauta sobe. */
    public void puxar(Direcao d, float hx, float hy, Color cor) {
        // Se ja havia uma puxada em andamento, guarda a posicao atual dos pes e deixa a diferenca sumir suavemente.
        for (int i = 0; i < 2; i++) peComp[i] = puxando ? peOff[i] : peComp[i];
        mundoInicio = alturaMundo;
        mundoAlvo += PASSO_PX;
        puxando = true;
        puxarT = 0f;
        dirPuxada = d;
        grabX = hx;
        grabY = hy;
        corPegada.set(cor);
        passoPar++;
        impulso = 0.6f;
    }

    /** Chegou ao destino: comemora com os bracos para cima. */
    public void celebrar() {
        celebrando = true;
        celebT = 0f;
        puxando = false;
        ritmo = 1f;
        alturaMundo = mundoAlvo;
    }

    /** Altura (px) que a parede ja rolou: o cenario usa este valor para descer junto com a escalada. */
    public float getAlturaMundo() { return alturaMundo; }

    /** Altura (px) que a parede terá rolado quando a puxada atual terminar. */
    public float getMundoAlvo() { return mundoAlvo; }

    public boolean isPuxando() { return puxando; }

    /** Acelera (>1) a puxada; usado na subida automatica ate o destino. */
    public void setRitmo(float r) { ritmo = r; }

    /** @param alvo agarradeira ativa (null = sem alvo). */
    public void atualizar(float dt, Direcao alvo, float[] holdX, float[] holdY) {
        alvoAtual = alvo;
        impulso = Math.max(0f, impulso - dt * 1.6f);
        if (celebrando) celebT += dt;
        if (puxando) {
            puxarT += dt * ritmo / DURACAO_PUXADA;
            if (puxarT >= 1f) {
                puxarT = 1f;
                puxando = false;
            }
        }

        // A parede rola para baixo enquanto o astronauta se puxa para cima.
        if (puxando) {
            float km = 1f - (1f - puxarT) * (1f - puxarT);
            alturaMundo = mundoInicio + (mundoAlvo - mundoInicio) * km;
        } else {
            alturaMundo = mundoAlvo;
        }
        calcularPes(dt);

        // Inclinacao do tronco para o lado da mao que trabalha.
        Direcao ref = puxando ? dirPuxada : alvo;
        float alvoLean = 0f;
        if (!celebrando && ref != null) {
            if (ref == Direcao.ESQUERDA) alvoLean = 0.17f;
            else if (ref == Direcao.DIREITA) alvoLean = -0.17f;
        }
        lean += (alvoLean - lean) * Math.min(1f, 9f * dt);

        // Para onde a cabeca olha: para a agarradeira (esquerda/direita) ou para cima.
        float alvoOlhar = 0f, alvoCima = 0f;
        if (celebrando) alvoCima = 1f;
        else if (ref == Direcao.ESQUERDA) alvoOlhar = -1f;
        else if (ref == Direcao.DIREITA) alvoOlhar = 1f;
        else if (ref == Direcao.ALTO) alvoCima = 1f;
        olhar += (alvoOlhar - olhar) * Math.min(1f, 8f * dt);
        olharCima += (alvoCima - olharCima) * Math.min(1f, 8f * dt);

        // Alvos das maos, em coordenadas LOCAIS do astronauta (ele e desenhado com escala ESCALA).
        // Posicoes de tela (agarradeiras) sao convertidas com lx() e ly().
        float restoEsqX = cx - (costas ? 46f : 58f), restoDirX = cx + (costas ? 46f : 58f);
        float restoY = baseY + (costas ? 124f : 30f);
        float aEx = restoEsqX, aEy = restoY, aDx = restoDirX, aDy = restoY;
        boolean agarrando = false;

        if (celebrando) {
            float onda = MathUtils.sin(celebT * 9f) * 10f;
            aEx = cx - 66f;
            aEy = baseY + 196f + onda;
            aDx = cx + 66f;
            aDy = baseY + 196f - onda;
        } else if (puxando) {
            float k = 1f - (1f - puxarT) * (1f - puxarT);            // easeOut: a agarradeira desce com o mundo
            float gy = grabY - AFUNDA * k;
            agarrando = true;
            switch (dirPuxada) {
                case ESQUERDA: aEx = lx(grabX); aEy = ly(gy); break;
                case DIREITA:  aDx = lx(grabX); aDy = ly(gy); break;
                default:       aEx = lx(grabX - 16f); aEy = ly(gy); aDx = lx(grabX + 16f); aDy = ly(gy); break;
            }
        } else if (alvo != null) {
            float hx = holdX[alvo.ordinal()];
            float hy = holdY[alvo.ordinal()];
            switch (alvo) {
                case ESQUERDA: aEx = lx(hx); aEy = ly(hy); break;
                case DIREITA:  aDx = lx(hx); aDy = ly(hy); break;
                default:       aEx = lx(hx - 16f); aEy = ly(hy); aDx = lx(hx + 16f); aDy = ly(hy); break;
            }
        }

        // Mao em repouso = agarrada em uma barra da parede (desenhada de costas).
        esqRepouso = !celebrando && aEx == restoEsqX && aEy == restoY;
        dirRepouso = !celebrando && aDx == restoDirX && aDy == restoY;

        // Mao agarrada acompanha exatamente a agarradeira; as demais suavizam o movimento.
        float suave = Math.min(1f, 14f * dt);
        boolean exato = agarrando && puxarT > 0.12f;
        if (exato && dirPuxada != Direcao.DIREITA) { maoEsqX = aEx; maoEsqY = aEy; }
        else { maoEsqX += (aEx - maoEsqX) * suave; maoEsqY += (aEy - maoEsqY) * suave; }
        if (exato && dirPuxada != Direcao.ESQUERDA) { maoDirX = aDx; maoDirY = aDy; }
        else { maoDirX += (aDx - maoDirX) * suave; maoDirY += (aDy - maoDirY) * suave; }
    }

    /**
     * Pes nos degraus: o pe "lider" sobe e pisa no proximo degrau; o pe de tras acompanha o degrau antigo
     * (que desce com a parede) ate o limite do alcance, solta, sobe e se junta ao outro. Ao fim de cada puxada
     * os dois pes ficam lado a lado, na mesma altura.
     */
    private void calcularPes(float dt) {
        float rolou = alturaMundo - mundoInicio;
        float total = Math.max(mundoAlvo - mundoInicio, ALCANCE_PE_TRAS + 1f);
        int lider = puxando ? pernaDoPasso() : -1;
        for (int k = 0; k < 2; k++) {
            float off = 0f;
            boolean ar = false;
            if (puxando) {
                if (k == lider) {
                    off = 0.65f * PASSO_PX * MathUtils.sin(MathUtils.PI * puxarT);
                    ar = off > 3f;
                } else if (rolou <= ALCANCE_PE_TRAS) {
                    off = -rolou;                                         // pisado no degrau antigo, que desce
                } else {
                    float u = Math.min(1f, (rolou - ALCANCE_PE_TRAS) / (total - ALCANCE_PE_TRAS));
                    off = -ALCANCE_PE_TRAS * (1f - u) + 10f * MathUtils.sin(MathUtils.PI * u);
                    ar = u > 0.05f && u < 0.95f;
                }
            }
            peOff[k] = off;
            peComp[k] *= Math.max(0f, 1f - 12f * dt);
            peNoAr[k] = ar;
        }
    }

    /** Converte uma posicao de tela para as coordenadas locais do astronauta (desfaz a escala). */
    private float lx(float sx) {
        return cx + (sx - cx) / ESCALA;
    }

    private float ly(float sy) {
        return baseY + (sy - baseY) / ESCALA;
    }

    // ================================================================ DESENHO

    /** Ponto no referencial do tronco: ox para a direita do astronauta, oy ao longo do tronco. Resultado em px, py. */
    private void p(float ox, float oy) {
        px = hipX + rx * ox + vx * oy;
        py = hipY + ry * ox + vy * oy;
    }

    /**
     * IK de 2 ossos. Origem (ax, ay), alvo (tx, ty). Resultado: junta em (jx, jy) e ponta em (ex, ey).
     * lado = +1 dobra a junta para a direita da tela, -1 para a esquerda (cotovelos e joelhos para fora).
     */
    private void ik(float ax, float ay, float tx, float ty, float l1, float l2, float lado) {
        float dx = tx - ax, dy = ty - ay;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 0.001f) {
            d = 0.001f;
            dx = 0f;
            dy = -0.001f;
        }
        float ux = dx / d, uy = dy / d;
        float dd = Math.min(d, (l1 + l2) * 0.999f);
        dd = Math.max(dd, Math.abs(l1 - l2) + 0.5f);
        float a = (l1 * l1 - l2 * l2 + dd * dd) / (2f * dd);
        float h2 = l1 * l1 - a * a;
        float h = h2 > 0f ? (float) Math.sqrt(h2) : 0f;
        float bx = ax + ux * a, by = ay + uy * a;
        float j1x = bx - uy * h, j1y = by + ux * h;
        float j2x = bx + uy * h, j2y = by - ux * h;
        if ((j1x - j2x) * lado >= 0f) {
            jx = j1x;
            jy = j1y;
        } else {
            jx = j2x;
            jy = j2y;
        }
        ex = ax + ux * dd;
        ey = ay + uy * dd;
    }

    public void desenhar(ShapeRenderer s, float tempo) {
        // Deslocamento vertical do corpo: puxada, pulo de comemoracao ou leve flutuacao.
        float bob = MathUtils.sin(tempo * 2f) * 3f;
        float dy = 0f;
        if (puxando) {
            dy = (costas ? 7f : 14f) * MathUtils.sin(puxarT * MathUtils.PI);
        } else if (celebrando) {
            dy = Math.abs(MathUtils.sin(celebT * 6f)) * 16f;
            bob = 0f;
        } else if (alvoAtual == Direcao.ALTO) {
            dy = 6f;
        }
        hipX = cx + (costas ? MathUtils.sin(tempo * 1.4f) * 0.8f : 0f);     // leve balanco do esforco
        hipY = baseY + bob + dy;
        vx = -MathUtils.sin(lean);
        vy = MathUtils.cos(lean);
        rx = MathUtils.cos(lean);
        ry = MathUtils.sin(lean);

        desenharPegada(s);                       // em coordenadas de tela, fora da escala

        // O corpo e desenhado em tamanho original e reduzido em torno do quadril.
        s.translate(cx, baseY, 0f);
        s.scale(ESCALA, ESCALA, 1f);
        s.translate(-cx, -baseY, 0f);
        if (costas) {
            desenharCostas(s, tempo, dy);
        } else {
            desenharMochila(s, tempo);
            desenharPernas(s, dy);
            desenharTronco(s, tempo);
            desenharBracos(s);
            desenharCabeca(s, tempo);
        }
        s.identity();
    }

    // ================================================================ VISTA DE COSTAS (escalando)

    /** Qual perna da o passo: movimento cruzado (mao esquerda puxa -> perna direita sobe, e vice-versa). */
    private int pernaDoPasso() {
        if (dirPuxada == Direcao.ESQUERDA) return 1;
        if (dirPuxada == Direcao.DIREITA) return 0;
        return passoPar % 2;
    }

    private void desenharCostas(ShapeRenderer s, float tempo, float dy) {
        desenharPernasCostas(s, dy);
        desenharTroncoCostas(s, tempo);
        desenharBracosCostas(s);
        desenharCabecaCostas(s, tempo);
    }

    private void desenharPernasCostas(ShapeRenderer s, float dy) {
        float saltoPe = celebrando ? dy * 0.9f : 0f;

        for (int k = 0; k < 2; k++) {
            float sg = (k == 0) ? -1f : 1f;
            p(sg * 16f, -4f);
            float hx = px, hy = py;

            // Pes sempre simetricos em x (cx +- 30) e apoiados nos degraus da parede (Cenario).
            boolean ar = peNoAr[k];
            float fx = cx + sg * 30f;
            float fy = ly(Y_SOLA + 10.5f + peOff[k] + peComp[k]) + saltoPe;
            ik(hx, hy, fx, fy, PERNA_1, PERNA_2, sg);

            s.setColor(0.90f, 0.91f, 0.96f, 1f);
            s.rectLine(hx, hy, jx, jy, 26f);
            s.rectLine(jx, jy, ex, ey, 22f);
            s.circle(hx, hy, 13f, 14);
            s.setColor(0.33f, 0.36f, 0.54f, 1f);
            s.circle(jx, jy, 10.5f, 14);                                  // joelheira

            // Bota vista por tras (calcanhar); a sola aparece quando o pe esta no ar.
            s.setColor(0.30f, 0.32f, 0.46f, 1f);
            Arte.retanguloArredondado(s, ex - 17f, ey - 14f, 34f, 18f, 6f);
            s.setColor(0.45f, 0.48f, 0.66f, 1f);
            s.rect(ex - 17f, ey - 6f, 34f, 3f);
            if (ar) {
                s.setColor(0.12f, 0.13f, 0.20f, 1f);
                Arte.retanguloArredondado(s, ex - 16f, ey - 20f, 32f, 9f, 3f);
                s.setColor(0.30f, 0.32f, 0.44f, 1f);
                for (int i = 0; i < 3; i++) s.rect(ex - 11f + i * 10f, ey - 19f, 4f, 7f);
            }
        }
    }

    private void desenharTroncoCostas(ShapeRenderer s, float tempo) {
        // Tronco (capsula)
        p(0f, 2f);
        float ax = px, ay = py;
        p(0f, 72f);
        float bx = px, by = py;
        s.setColor(0.96f, 0.96f, 1f, 1f);
        s.circle(ax, ay, 36f, 28);
        s.circle(bx, by, 37f, 28);
        s.rectLine(ax, ay, bx, by, 74f);

        // Cinto
        s.setColor(0.33f, 0.36f, 0.54f, 1f);
        p(-35f, 14f);
        float c0x = px, c0y = py;
        p(35f, 14f);
        s.rectLine(c0x, c0y, px, py, 9f);

        // Mochila de suporte de vida (moldura e corpo)
        p(0f, 10f);
        float m0x = px, m0y = py;
        p(0f, 70f);
        s.setColor(0.52f, 0.55f, 0.70f, 1f);
        s.rectLine(m0x, m0y, px, py, 70f);
        s.setColor(0.82f, 0.84f, 0.92f, 1f);
        s.rectLine(m0x, m0y, px, py, 62f);

        // Dois tanques com remendo dourado e valvula
        for (int k = 0; k < 2; k++) {
            float sg = (k == 0) ? -1f : 1f;
            p(sg * 17f, 16f);
            float t0x = px, t0y = py;
            p(sg * 17f, 64f);
            float t1x = px, t1y = py;
            s.setColor(0.93f, 0.94f, 0.98f, 1f);
            s.rectLine(t0x, t0y, t1x, t1y, 22f);
            s.circle(t0x, t0y, 11f, 14);
            s.circle(t1x, t1y, 11f, 14);
            s.setColor(0.92f, 0.76f, 0.30f, 1f);
            p(sg * 17f, 28f);
            float g0x = px, g0y = py;
            p(sg * 17f, 44f);
            s.rectLine(g0x, g0y, px, py, 14f);
            s.setColor(0.35f, 0.38f, 0.55f, 1f);
            p(sg * 17f, 68f);
            s.circle(px, py, 4.5f, 10);
        }

        // Painel central com luzes
        s.setColor(0.28f, 0.31f, 0.46f, 1f);
        p(0f, 22f);
        float q0x = px, q0y = py;
        p(0f, 60f);
        s.rectLine(q0x, q0y, px, py, 18f);
        p(0f, 54f);
        s.setColor(0.95f, 0.30f, 0.30f, 1f);
        s.circle(px, py, 3.5f, 10);
        p(0f, 46f);
        s.setColor(0.35f, 0.95f, 0.50f, 0.5f + 0.5f * MathUtils.sin(tempo * 4f));
        s.circle(px, py, 3.5f, 10);
        p(0f, 38f);
        s.setColor(0.30f, 0.80f, 1f, 1f);
        s.circle(px, py, 3.5f, 10);

        // Mangueiras laterais
        s.setColor(0.28f, 0.30f, 0.43f, 1f);
        p(-28f, 16f);
        float h0x = px, h0y = py;
        p(-39f, 4f);
        s.rectLine(h0x, h0y, px, py, 4f);
        p(28f, 16f);
        h0x = px;
        h0y = py;
        p(39f, 4f);
        s.rectLine(h0x, h0y, px, py, 4f);

        // Pescoco
        s.setColor(0.40f, 0.43f, 0.60f, 1f);
        p(0f, 78f);
        float n0x = px, n0y = py;
        p(0f, 94f);
        s.rectLine(n0x, n0y, px, py, 46f);
    }

    private void desenharBracosCostas(ShapeRenderer s) {
        for (int k = 0; k < 2; k++) {
            float sg = (k == 0) ? -1f : 1f;
            p(sg * 36f, 62f);
            float ox = px, oy = py;
            float mx = (k == 0) ? maoEsqX : maoDirX;
            float my = (k == 0) ? maoEsqY : maoDirY;
            ik(ox, oy, mx, my, BRACO_1, BRACO_2, sg);

            // Barra da parede onde a mao descansa agarrada.
            if (k == 0 ? esqRepouso : dirRepouso) {
                s.setColor(0.24f, 0.26f, 0.40f, 1f);
                s.rectLine(ex - 17f, ey + 2f, ex + 17f, ey + 2f, 8f);
                s.circle(ex - 17f, ey + 2f, 4f, 8);
                s.circle(ex + 17f, ey + 2f, 4f, 8);
                s.setColor(0.45f, 0.48f, 0.66f, 1f);
                s.rectLine(ex - 15f, ey + 4.5f, ex + 15f, ey + 4.5f, 2f);
            }

            s.setColor(0.90f, 0.91f, 0.96f, 1f);
            s.rectLine(ox, oy, jx, jy, 19f);
            s.rectLine(jx, jy, ex, ey, 17f);
            s.circle(ox, oy, 12.5f, 14);                                  // ombro
            s.setColor(0.33f, 0.36f, 0.54f, 1f);
            s.circle(jx, jy, 8.5f, 14);                                   // cotoveleira
            s.circle(ex, ey, 12f, 16);                                    // punho

            // Luva vista por tras: dorso da mao e dedos apontando na direcao do antebraco.
            float dxn = ex - jx, dyn = ey - jy;
            float len = (float) Math.sqrt(dxn * dxn + dyn * dyn);
            if (len < 0.001f) {
                dxn = 0f;
                dyn = 1f;
            } else {
                dxn /= len;
                dyn /= len;
            }
            s.setColor(0.97f, 0.72f, 0.20f, 1f);
            s.circle(ex, ey, 10.5f, 16);
            s.setColor(0.99f, 0.78f, 0.28f, 1f);
            for (int i = 0; i < 4; i++) {
                float off = (i - 1.5f) * 5.2f;
                s.circle(ex + dxn * 10f - dyn * off, ey + dyn * 10f + dxn * off, 3.4f, 8);
            }
        }
    }

    private void desenharCabecaCostas(ShapeRenderer s, float tempo) {
        p(0f, 126f);
        float hx = px + olhar * 5f;
        float hy = py + olharCima * 5f;

        // Casco visto por tras
        s.setColor(0.97f, 0.97f, 1f, 1f);
        s.circle(hx, hy, 38f, 36);
        s.setColor(0.86f, 0.88f, 0.96f, 1f);
        s.circle(hx - 12f, hy - 10f, 22f, 24);
        s.setColor(0.97f, 0.97f, 1f, 1f);
        s.circle(hx, hy + 2f, 34f, 36);
        s.setColor(0.92f, 0.45f, 0.20f, 1f);
        s.rectLine(hx - 27f, hy + 6f, hx + 27f, hy + 6f, 6f);              // faixa do capacete
        s.setColor(1f, 0.25f, 0.25f, 0.5f + 0.5f * MathUtils.sin(tempo * 4f));
        s.circle(hx, hy + 22f, 3.5f, 10);                                  // luz traseira

        // Quando a cabeca gira, uma fatia do visor aparece na lateral.
        float ao = Math.abs(olhar);
        if (ao > 0.25f) {
            float a = Math.min(1f, (ao - 0.25f) / 0.5f);
            float lado = Math.signum(olhar);
            s.setColor(0.07f, 0.12f, 0.32f, a);
            s.ellipse(hx + lado * 30f - 7f, hy - 14f, 14f, 28f);
            s.setColor(0.20f, 0.42f, 0.85f, a * 0.9f);
            s.ellipse(hx + lado * 31f - 4f, hy - 8f, 8f, 16f);
        }

        // Antena
        s.setColor(0.55f, 0.58f, 0.72f, 1f);
        s.rectLine(hx + 28f, hy + 22f, hx + 38f, hy + 44f, 3f);
        s.circle(hx + 38f, hy + 46f, 3.5f, 8);
    }

    /** Brilho da agarradeira que o astronauta esta segurando (desce junto com a mao e some). */
    private void desenharPegada(ShapeRenderer s) {
        if (!puxando) return;
        float k = 1f - (1f - puxarT) * (1f - puxarT);
        float gy = grabY - AFUNDA * k;
        float fade = 1f - puxarT;
        Arte.brilho(s, grabX, gy, 70f, corPegada, fade);
        s.setColor(corPegada.r, corPegada.g, corPegada.b, fade);
        s.circle(grabX, gy, 22f, 24);
    }

    private void desenharMochila(ShapeRenderer s, float tempo) {
        p(0f, 6f);
        float ax = px, ay = py;
        p(0f, 70f);
        s.setColor(0.50f, 0.54f, 0.68f, 1f);
        s.rectLine(ax, ay, px, py, 94f);

        // Chamas dos dois propulsores
        float chama = 8f + 5f * MathUtils.sin(tempo * 30f) + impulso * 46f;
        for (int k = 0; k < 2; k++) {
            float sg = (k == 0) ? -1f : 1f;
            p(sg * 26f, 4f);
            float nx = px, ny = py;
            s.setColor(0.36f, 0.39f, 0.55f, 1f);
            s.rectLine(nx - rx * 12f, ny - ry * 12f, nx + rx * 12f, ny + ry * 12f, 10f);
            s.setColor(1f, 0.55f, 0.10f, 0.9f);
            s.triangle(nx - rx * 10f, ny - ry * 10f, nx + rx * 10f, ny + ry * 10f, nx - vx * chama, ny - vy * chama);
            s.setColor(1f, 0.95f, 0.50f, 0.95f);
            s.triangle(nx - rx * 5f, ny - ry * 5f, nx + rx * 5f, ny + ry * 5f, nx - vx * chama * 0.6f, ny - vy * chama * 0.6f);
        }
    }

    private void desenharPernas(ShapeRenderer s, float dy) {
        // Qual perna da o passo: alterna a cada puxada.
        int pernaPasso = passoPar % 2;
        float levante = puxando ? 46f * MathUtils.sin(puxarT * MathUtils.PI) : 0f;
        float saltoPe = celebrando ? dy * 0.9f : 0f;

        for (int k = 0; k < 2; k++) {
            float sg = (k == 0) ? -1f : 1f;
            p(sg * 16f, -4f);
            float hx = px, hy = py;

            float fx = cx + sg * 32f;
            float fy = baseY - 100f + saltoPe;
            if (puxando && k == pernaPasso) {
                fy += levante;
                fx -= sg * 10f * MathUtils.sin(puxarT * MathUtils.PI);   // o pe que sobe vai um pouco para dentro
            }
            ik(hx, hy, fx, fy, PERNA_1, PERNA_2, sg);

            s.setColor(0.90f, 0.91f, 0.96f, 1f);
            s.rectLine(hx, hy, jx, jy, 25f);
            s.rectLine(jx, jy, ex, ey, 22f);
            s.circle(jx, jy, 12f, 14);
            s.circle(hx, hy, 12.5f, 14);
            // Bota
            s.setColor(0.30f, 0.32f, 0.46f, 1f);
            Arte.retanguloArredondado(s, ex - 18f, ey - 14f, 36f, 18f, 6f);
            s.setColor(0.18f, 0.19f, 0.30f, 1f);
            s.rect(ex - 18f, ey - 14f, 36f, 4f);
        }
    }

    private void desenharTronco(ShapeRenderer s, float tempo) {
        p(0f, 2f);
        float ax = px, ay = py;
        p(0f, 72f);
        float bx = px, by = py;

        s.setColor(0.96f, 0.96f, 1f, 1f);
        s.circle(ax, ay, 36f, 28);
        s.circle(bx, by, 37f, 28);
        s.rectLine(ax, ay, bx, by, 74f);

        // Sombreamento lateral
        s.setColor(0.84f, 0.86f, 0.94f, 1f);
        p(22f, 8f);
        float sx0 = px, sy0 = py;
        p(22f, 68f);
        s.rectLine(sx0, sy0, px, py, 22f);

        // Cinto
        s.setColor(0.33f, 0.36f, 0.54f, 1f);
        p(-35f, 14f);
        float cx0 = px, cy0 = py;
        p(35f, 14f);
        s.rectLine(cx0, cy0, px, py, 9f);

        // Painel de controle
        s.setColor(0.16f, 0.18f, 0.32f, 1f);
        p(0f, 34f);
        float pxa = px, pya = py;
        p(0f, 56f);
        s.rectLine(pxa, pya, px, py, 40f);
        p(-10f, 50f);
        s.setColor(0.95f, 0.30f, 0.30f, 1f);
        s.circle(px, py, 4f, 10);
        p(0f, 50f);
        s.setColor(0.35f, 0.95f, 0.50f, 1f);
        s.circle(px, py, 4f, 10);
        p(10f, 50f);
        s.setColor(0.30f, 0.80f, 1f, 0.6f + 0.4f * MathUtils.sin(tempo * 5f));
        s.circle(px, py, 4f, 10);
        s.setColor(0.45f, 0.50f, 0.70f, 1f);
        p(-14f, 40f);
        float ta = px, tb = py;
        p(14f, 40f);
        s.rectLine(ta, tb, px, py, 3f);

        // Pescoco
        s.setColor(0.40f, 0.43f, 0.60f, 1f);
        p(0f, 78f);
        float na = px, nb = py;
        p(0f, 94f);
        s.rectLine(na, nb, px, py, 46f);
    }

    private void desenharBracos(ShapeRenderer s) {
        for (int k = 0; k < 2; k++) {
            float sg = (k == 0) ? -1f : 1f;
            p(sg * 36f, 62f);
            float ox = px, oy = py;
            float mx = (k == 0) ? maoEsqX : maoDirX;
            float my = (k == 0) ? maoEsqY : maoDirY;
            ik(ox, oy, mx, my, BRACO_1, BRACO_2, sg);

            s.setColor(0.90f, 0.91f, 0.96f, 1f);
            s.rectLine(ox, oy, jx, jy, 19f);
            s.rectLine(jx, jy, ex, ey, 17f);
            s.circle(ox, oy, 11f, 14);
            s.circle(jx, jy, 9.5f, 14);
            s.setColor(0.33f, 0.36f, 0.54f, 1f);
            s.circle(ex, ey, 12f, 16);
            s.setColor(0.97f, 0.72f, 0.20f, 1f);
            s.circle(ex, ey, 9.5f, 16);
        }
    }

    private void desenharCabeca(ShapeRenderer s, float tempo) {
        p(0f, 126f);
        float hx = px, hy = py;

        s.setColor(0.97f, 0.97f, 1f, 1f);
        s.circle(hx, hy, 38f, 36);
        s.setColor(0.86f, 0.88f, 0.96f, 1f);
        s.circle(hx + 15f, hy - 8f, 22f, 24);
        s.setColor(0.97f, 0.97f, 1f, 1f);
        s.circle(hx, hy, 35f, 36);

        // Visor
        s.setColor(0.07f, 0.12f, 0.32f, 1f);
        s.ellipse(hx - 28f, hy - 15f, 56f, 38f);
        s.setColor(0.20f, 0.42f, 0.85f, 1f);
        s.ellipse(hx - 22f, hy - 7f, 40f, 22f);
        s.setColor(1f, 1f, 1f, 0.75f);
        s.circle(hx - 13f, hy + 14f, 4f, 10);
        s.circle(hx - 4f, hy + 17f, 2.2f, 8);

        // Antena
        s.setColor(0.55f, 0.58f, 0.72f, 1f);
        s.rectLine(hx + 28f, hy + 22f, hx + 40f, hy + 46f, 3f);
        s.setColor(1f, 0.25f, 0.25f, 0.5f + 0.5f * MathUtils.sin(tempo * 4f));
        s.circle(hx + 40f, hy + 48f, 4f, 10);
    }
}
