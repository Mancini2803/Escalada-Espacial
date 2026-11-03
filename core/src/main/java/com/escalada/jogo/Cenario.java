package com.escalada.jogo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.escalada.Main;

/**
 * Fundo do jogo: espaco em degrade, nebulosas, planeta, estrelas com paralaxe e a estrutura espacial
 * (paineis, vigas, janelas e luzes) que desce conforme o astronauta sobe. Cada fase tem um tema de cor.
 */
public class Cenario {
    private static final float ALTURA_SEGMENTO = 90f;
    private static final float EST_X0 = 130f;      // borda esquerda da estrutura
    private static final float EST_X1 = 830f;      // borda direita

    //                                    fase 1 (azul)        fase 2 (roxo)         fase 3 (alerta)
    private static final float[][] TOPO = { {0.00f, 0.04f, 0.16f}, {0.08f, 0.03f, 0.18f}, {0.12f, 0.00f, 0.03f} };
    private static final float[][] BASE = { {0.06f, 0.28f, 0.62f}, {0.38f, 0.14f, 0.60f}, {0.62f, 0.08f, 0.08f} };
    // Estrutura (parede) de cada fase: fundo, paineis (baixo/cima), vigas, brilho da viga e moldura.
    private static final float[][] EST_FUNDO = { {0.04f, 0.10f, 0.22f}, {0.10f, 0.06f, 0.20f}, {0.16f, 0.04f, 0.06f} };
    private static final float[][] EST_PB = { {0.08f, 0.20f, 0.40f}, {0.20f, 0.12f, 0.36f}, {0.30f, 0.08f, 0.10f} };
    private static final float[][] EST_PC = { {0.12f, 0.30f, 0.56f}, {0.30f, 0.18f, 0.52f}, {0.45f, 0.12f, 0.14f} };
    private static final float[][] EST_VIGA = { {0.28f, 0.50f, 0.80f}, {0.55f, 0.36f, 0.82f}, {0.72f, 0.26f, 0.22f} };
    private static final float[][] EST_BRILHO = { {0.55f, 0.80f, 1.00f}, {0.80f, 0.62f, 1.00f}, {1.00f, 0.55f, 0.40f} };
    private static final float[][] EST_MOLDURA = { {0.20f, 0.40f, 0.70f}, {0.42f, 0.28f, 0.68f}, {0.85f, 0.70f, 0.10f} };
    private static final float[][] NEB_A = { {0.15f, 0.35f, 0.90f}, {0.60f, 0.20f, 0.90f}, {0.90f, 0.20f, 0.20f} };
    private static final float[][] NEB_B = { {0.10f, 0.75f, 0.85f}, {0.95f, 0.30f, 0.65f}, {1.00f, 0.60f, 0.10f} };
    private static final float[][] PLANETA = { {0.30f, 0.38f, 0.78f}, {0.62f, 0.32f, 0.70f}, {0.75f, 0.32f, 0.25f} };

    private final Color cTopo = new Color();
    private final Color cBase = new Color();
    private final Color cNebA = new Color();
    private final Color cNebB = new Color();
    private final Color cPlaneta = new Color();
    private final Color cPreto0 = new Color(0f, 0f, 0f, 0f);
    private final Color cPreto1 = new Color(0f, 0f, 0f, 0.55f);
    private final Color cPainelBaixo = new Color(0.10f, 0.12f, 0.22f, 1f);
    private final Color cPainelCima = new Color(0.15f, 0.17f, 0.30f, 1f);
    private final Color cAcento = new Color();

    private final float[] estX = new float[90];
    private final float[] estY = new float[90];
    private final float[] estTam = new float[90];
    // Asteroides decorativos (formas irregulares que giram e derivam)
    private final float[] astX = { 80f, 300f, 520f, 700f, 880f, 150f };
    private final float[] astY = { 400f, 120f, 300f, 480f, 200f, 60f };
    private final float[] astTam = { 16f, 11f, 20f, 13f, 18f, 9f };
    private final float[] astVel = { 9f, 14f, 6f, 11f, 8f, 16f };
    private final float[][] astRaio = new float[6][8];
    private int temaAtual;
    /** false no menu: os degraus so fazem sentido durante a escalada. */
    public boolean mostrarDegraus = true;
    private static final float PE_X = 480f;            // eixo do astronauta (igual a ASTRO_X)

    private final float[] nebX = { 150f, 420f, 700f, 880f, 60f };
    private final float[] nebY = { 120f, 360f, 220f, 470f, 430f };

    public Cenario() {
        for (int i = 0; i < estX.length; i++) {
            estX[i] = MathUtils.random(0f, Main.LARGURA);
            estY[i] = MathUtils.random(0f, Main.ALTURA);
            estTam[i] = 1.2f + (i % 3) * 0.9f;
        }
        for (int i = 0; i < astRaio.length; i++) {
            for (int j = 0; j < astRaio[i].length; j++) astRaio[i][j] = MathUtils.random(0.72f, 1.08f);
        }
    }

    /** Altura (y na tela) da superficie da plataforma de destino; ela desce conforme o astronauta sobe. */
    public float yPlataforma(float alt, float alturaDestino) {
        return Astronauta.Y_SOLA + (alturaDestino - alt);
    }

    /** Posicao horizontal central da placa "Destino Final" e deslocamento vertical acima da plataforma. */
    public static final float SINAL_X = 255f;
    public static final float SINAL_DY = 150f;
    public static final float SINAL_ALTURA = 54f;

    public void desenhar(ShapeRenderer s, float alt, float t, int tema, float alturaDestino) {
        int k = MathUtils.clamp(tema, 0, TOPO.length - 1);
        cTopo.set(TOPO[k][0], TOPO[k][1], TOPO[k][2], 1f);
        cBase.set(BASE[k][0], BASE[k][1], BASE[k][2], 1f);
        cNebA.set(NEB_A[k][0], NEB_A[k][1], NEB_A[k][2], 1f);
        cNebB.set(NEB_B[k][0], NEB_B[k][1], NEB_B[k][2], 1f);
        cPlaneta.set(PLANETA[k][0], PLANETA[k][1], PLANETA[k][2], 1f);
        cAcento.set(NEB_B[k][0], NEB_B[k][1], NEB_B[k][2], 1f);

        // Espaco em degrade
        s.rect(0, 0, Main.LARGURA, Main.ALTURA, cBase, cBase, cTopo, cTopo);

        // Nebulosas (movem-se bem devagar)
        float ciclo = Main.ALTURA + 400f;
        for (int i = 0; i < nebX.length; i++) {
            float y = ((nebY[i] - alt * 0.06f) % ciclo + ciclo) % ciclo - 200f;
            Arte.brilho(s, nebX[i], y, 230f, (i % 2 == 0) ? cNebA : cNebB, 1.0f);
        }

        // Planeta gigante espiando pela direita
        float py = 420f - alt * 0.05f;
        Arte.brilho(s, 900f, py, 190f, cAcento, 0.9f);
        s.setColor(cPlaneta);
        s.circle(900f, py, 100f, 60);
        s.setColor(1f, 1f, 1f, 0.07f);
        s.circle(870f, py + 25f, 60f, 40);
        s.circle(860f, py + 32f, 42f, 36);
        s.circle(852f, py + 38f, 26f, 30);
        s.setColor(0f, 0f, 0f, 0.10f);
        s.circle(940f, py - 40f, 70f, 40);

        // Estrelas em 3 camadas de profundidade, com brilho pulsante
        float[] vel = { 0.08f, 0.20f, 0.40f };
        for (int i = 0; i < estX.length; i++) {
            int camada = i % 3;
            float y = ((estY[i] - alt * vel[camada]) % Main.ALTURA + Main.ALTURA) % Main.ALTURA;
            float brilho = 0.45f + 0.45f * MathUtils.sin(t * (1.5f + camada) + i);
            s.setColor(1f, 1f, 1f, brilho);
            s.rect(estX[i], y, estTam[i], estTam[i]);
        }

        temaAtual = k;
        cPainelBaixo.set(EST_PB[k][0], EST_PB[k][1], EST_PB[k][2], 1f);
        cPainelCima.set(EST_PC[k][0], EST_PC[k][1], EST_PC[k][2], 1f);
        desenharDecoracao(s, alt, t, k);

        float yPlat = yPlataforma(alt, alturaDestino);
        desenharEstrutura(s, alt, t, yPlat);
        desenharDestino(s, t, tema, yPlat);

        // Fase 3: alerta vermelho pulsando sobre a cena toda.
        if (k == 2) {
            s.setColor(1f, 0.05f, 0.05f, 0.05f + 0.07f * (0.5f + 0.5f * MathUtils.sin(t * 5f)));
            s.rect(0, 0, Main.LARGURA, Main.ALTURA);
        }

        // Vinheta (escurece as bordas e foca o centro)
        s.rect(0, 0, Main.LARGURA, 70f, cPreto1, cPreto1, cPreto0, cPreto0);
        s.rect(0, Main.ALTURA - 70f, Main.LARGURA, 70f, cPreto0, cPreto0, cPreto1, cPreto1);
        s.rect(0, 0, 90f, Main.ALTURA, cPreto1, cPreto0, cPreto0, cPreto1);
        s.rect(Main.LARGURA - 90f, 0, 90f, Main.ALTURA, cPreto0, cPreto1, cPreto1, cPreto0);
    }

    // ================================================================ DECORACAO ESPACIAL

    private static float volta(float v, float m) {
        return ((v % m) + m) % m;
    }

    /** Planeta com aneis, OVNIs, aliens flutuando, asteroides, estrelas cadentes e um satelite. */
    private void desenharDecoracao(ShapeRenderer s, float alt, float t, int tema) {
        planetaEsquerdo(s, alt, tema);

        // Estrelas cadentes (duas, em ciclos diferentes)
        for (int n = 0; n < 2; n++) {
            float periodo = 6.5f + n * 3.2f;
            float u = (t % periodo) / periodo;
            if (u > 0.14f) continue;
            float p = u / 0.14f;
            int ciclo = (int) (t / periodo);
            float sx = 300f + ((ciclo * 197 + n * 263) % 640);
            float sy = 330f + ((ciclo * 89 + n * 151) % 190);
            float hx = sx - 380f * p, hy = sy - 210f * p;
            float a = MathUtils.sin(p * MathUtils.PI);
            s.setColor(0.8f, 0.95f, 1f, 0.45f * a);
            s.triangle(hx, hy, hx + 130f, hy + 66f, hx + 120f, hy + 82f);
            s.setColor(1f, 1f, 1f, a);
            s.circle(hx, hy, 3.2f, 10);
        }

        // Asteroides derivando para a esquerda
        int nAst = (tema == 0) ? 3 : (tema == 1 ? 6 : 5);
        for (int i = 0; i < nAst; i++) {
            float x = volta(astX[i] - t * astVel[i], Main.LARGURA + 200f) - 100f;
            float y = volta(astY[i] - alt * 0.10f, Main.ALTURA + 200f) - 100f;
            asteroide(s, x, y, astTam[i], astRaio[i], t * (0.3f + i * 0.07f), tema);
        }

        // Satelite cruzando o topo (fases 1 e 2)
        if (tema < 2) {
            float x = volta(t * 14f + 120f, Main.LARGURA + 160f) - 80f;
            float y = 488f + MathUtils.sin(t * 0.5f) * 10f - alt * 0.02f;
            satelite(s, x, y, t);
        }

        // OVNIs
        int nUfo = (tema == 2) ? 3 : 2;
        for (int i = 0; i < nUfo; i++) {
            float vel = 18f + i * 9f;
            float x = volta(t * vel + i * 410f, Main.LARGURA + 300f) - 150f;
            if (i % 2 == 1) x = Main.LARGURA - x;
            float y = volta(210f + i * 170f - alt * 0.05f, Main.ALTURA + 160f) - 80f + MathUtils.sin(t * 1.4f + i) * 14f;
            ovni(s, x, y, 0.8f + 0.25f * (i % 2), t, i);
        }

        // Alienigenas flutuando nas margens (descem conforme o astronauta sobe)
        float[] ax = { 62f, 898f, 84f, 880f };
        float[] ay = { 420f, 250f, 150f, 470f };
        for (int i = 0; i < ax.length; i++) {
            float x = ax[i] + MathUtils.sin(t * (0.9f - i * 0.1f) + i) * 10f;
            float y = volta(ay[i] - alt * 0.12f, Main.ALTURA + 160f) - 80f;
            alien(s, x, y + MathUtils.sin(t * 1.3f + i * 2f) * 6f, 0.95f + 0.12f * (i % 2), (tema + i) % 3, t, i);
        }
    }

    private void planetaEsquerdo(ShapeRenderer s, float alt, int tema) {
        float cx = 20f;
        float cy = 330f - alt * 0.04f;
        float r = 78f;
        boolean aneis = tema != 1;
        if (aneis) {
            s.setColor(0.88f, 0.82f, 0.68f, 0.45f);
            Arte.arcoEliptico(s, cx, cy, 160f, 32f, 0.35f, 0f, 180f, 6f, 40);     // parte de tras do anel
        }
        if (tema == 0) s.setColor(0.32f, 0.52f, 0.90f, 1f);
        else if (tema == 1) s.setColor(0.60f, 0.57f, 0.70f, 1f);
        else s.setColor(0.80f, 0.34f, 0.20f, 1f);
        s.circle(cx, cy, r, 48);
        s.setColor(1f, 1f, 1f, 0.07f);                                              // luz vindo da direita
        s.circle(cx + 20f, cy + 14f, r * 0.62f, 36);
        s.circle(cx + 28f, cy + 20f, r * 0.38f, 30);
        if (tema == 1) {                                                           // crateras
            s.setColor(0.45f, 0.42f, 0.55f, 1f);
            s.circle(cx + 44f, cy + 20f, 12f, 16);
            s.circle(cx + 26f, cy - 28f, 8f, 14);
            s.circle(cx + 56f, cy - 14f, 6f, 12);
        }
        if (aneis) {
            s.setColor(0.92f, 0.86f, 0.72f, 0.75f);
            Arte.arcoEliptico(s, cx, cy, 160f, 32f, 0.35f, 180f, 360f, 6f, 40);   // parte da frente do anel
        }
    }

    private void asteroide(ShapeRenderer s, float x, float y, float tam, float[] raios, float rot, int tema) {
        float r0 = 0.42f + tema * 0.05f, g0 = 0.38f, b0 = 0.36f + (tema == 1 ? 0.10f : 0f);
        int n = raios.length;
        for (int j = 0; j < n; j++) {
            float a0 = rot + j * MathUtils.PI2 / n;
            float a1 = rot + (j + 1) * MathUtils.PI2 / n;
            float ra = raios[j] * tam, rb = raios[(j + 1) % n] * tam;
            s.setColor(r0 * (0.8f + 0.2f * (j % 2)), g0 * (0.8f + 0.2f * (j % 2)), b0 * (0.8f + 0.2f * (j % 2)), 1f);
            s.triangle(x, y, x + MathUtils.cos(a0) * ra, y + MathUtils.sin(a0) * ra,
                       x + MathUtils.cos(a1) * rb, y + MathUtils.sin(a1) * rb);
        }
        s.setColor(0.25f, 0.22f, 0.22f, 1f);
        s.circle(x + MathUtils.cos(rot + 1f) * tam * 0.3f, y + MathUtils.sin(rot + 1f) * tam * 0.3f, tam * 0.2f, 10);
        s.circle(x + MathUtils.cos(rot + 3.4f) * tam * 0.35f, y + MathUtils.sin(rot + 3.4f) * tam * 0.35f, tam * 0.14f, 8);
    }

    private void satelite(ShapeRenderer s, float x, float y, float t) {
        s.setColor(0.30f, 0.50f, 0.85f, 1f);                  // paineis solares
        s.rect(x - 34f, y - 5f, 24f, 10f);
        s.rect(x + 10f, y - 5f, 24f, 10f);
        s.setColor(0.65f, 0.80f, 1f, 1f);
        for (int k = 1; k < 3; k++) {
            s.rect(x - 34f + k * 8f, y - 5f, 1f, 10f);
            s.rect(x + 10f + k * 8f, y - 5f, 1f, 10f);
        }
        s.setColor(0.72f, 0.74f, 0.82f, 1f);                  // corpo
        s.rect(x - 9f, y - 8f, 18f, 16f);
        s.setColor(0.55f, 0.58f, 0.68f, 1f);
        s.rectLine(x, y + 8f, x + 6f, y + 20f, 2f);          // antena
        s.setColor(1f, 0.3f, 0.3f, 0.4f + 0.6f * (0.5f + 0.5f * MathUtils.sin(t * 5f)));
        s.circle(x + 6f, y + 21f, 2.5f, 8);
    }

    private void ovni(ShapeRenderer s, float x, float y, float e, float t, int i) {
        // feixe de luz
        float pis = 0.09f + 0.06f * MathUtils.sin(t * 3f + i);
        s.setColor(cAcento.r, cAcento.g, cAcento.b, pis);
        s.triangle(x - 10f * e, y - 6f * e, x + 10f * e, y - 6f * e, x + 46f * e, y - 120f * e);
        s.triangle(x - 10f * e, y - 6f * e, x + 46f * e, y - 120f * e, x - 46f * e, y - 120f * e);
        // piloto dentro da cupula
        s.setColor(0.38f, 0.85f, 0.45f, 1f);
        s.circle(x, y + 9f * e, 7f * e, 14);
        s.setColor(0f, 0f, 0f, 1f);
        s.circle(x - 2.6f * e, y + 10f * e, 1.5f * e, 8);
        s.circle(x + 2.6f * e, y + 10f * e, 1.5f * e, 8);
        // cupula de vidro
        s.setColor(0.65f, 0.92f, 1f, 0.45f);
        s.circle(x, y + 8f * e, 15f * e, 20);
        // corpo
        s.setColor(0.62f, 0.66f, 0.78f, 1f);
        s.ellipse(x - 38f * e, y - 10f * e, 76f * e, 22f * e);
        s.setColor(0.42f, 0.46f, 0.60f, 1f);
        s.ellipse(x - 38f * e, y - 10f * e, 76f * e, 10f * e);
        // luzes piscando
        for (int k = -1; k <= 1; k++) {
            float on = 0.5f + 0.5f * MathUtils.sin(t * 6f + k * 2f + i);
            if (k == -1) s.setColor(1f, 0.35f, 0.35f, 0.4f + 0.6f * on);
            else if (k == 0) s.setColor(0.4f, 1f, 0.5f, 0.4f + 0.6f * on);
            else s.setColor(0.4f, 0.7f, 1f, 0.4f + 0.6f * on);
            s.circle(x + k * 22f * e, y - 2f * e, 3.4f * e, 8);
        }
    }

    /**
     * Alienigena (so a cabeca) em tres tipos: 0 = verde de duas antenas, 1 = roxo de um olho so, 2 = vermelho
     * de tentaculos. (x, y) = centro da cabeca; e = escala.
     */
    private void alien(ShapeRenderer s, float x, float y, float e, int tipo, float t, int fase) {
        float olha = MathUtils.sin(t * 1.1f + fase) * 1.8f * e;
        boolean pisca = ((t * 0.8f + fase * 0.7f) % 4f) < 0.12f;
        if (tipo == 0) {
            s.setColor(0.30f, 0.70f, 0.36f, 1f);
            s.rectLine(x - 10f * e, y + 16f * e, x - 16f * e, y + 32f * e, 2.6f * e);
            s.rectLine(x + 10f * e, y + 16f * e, x + 16f * e, y + 32f * e, 2.6f * e);
            s.setColor(1f, 0.9f, 0.3f, 1f);
            s.circle(x - 16f * e, y + 34f * e, 4f * e, 10);
            s.circle(x + 16f * e, y + 34f * e, 4f * e, 10);
            s.setColor(0.38f, 0.85f, 0.45f, 1f);
            s.ellipse(x - 24f * e, y - 18f * e, 48f * e, 40f * e);
            for (int k = -1; k <= 1; k += 2) {
                s.setColor(1f, 1f, 1f, 1f);
                s.ellipse(x + k * 10f * e - 8f * e, y - 6f * e, 16f * e, 20f * e);
                s.setColor(0.05f, 0.08f, 0.05f, 1f);
                s.circle(x + k * 10f * e + olha, y + 2f * e, 4.5f * e, 10);
                s.setColor(1f, 1f, 1f, 1f);
                s.circle(x + k * 10f * e + olha + 1.5f * e, y + 4f * e, 1.5f * e, 6);
                if (pisca) {
                    s.setColor(0.38f, 0.85f, 0.45f, 1f);
                    s.ellipse(x + k * 10f * e - 9f * e, y - 7f * e, 18f * e, 22f * e);
                }
            }
            s.setColor(0.10f, 0.22f, 0.12f, 1f);
            s.ellipse(x - 6f * e, y - 14f * e, 12f * e, 6f * e);
        } else if (tipo == 1) {
            s.setColor(0.45f, 0.22f, 0.65f, 1f);
            s.triangle(x - 18f * e, y + 14f * e, x - 8f * e, y + 20f * e, x - 22f * e, y + 34f * e);
            s.triangle(x + 18f * e, y + 14f * e, x + 8f * e, y + 20f * e, x + 22f * e, y + 34f * e);
            s.setColor(0.62f, 0.35f, 0.85f, 1f);
            s.circle(x, y, 24f * e, 28);
            s.setColor(1f, 1f, 1f, 1f);
            s.circle(x, y + 4f * e, 13f * e, 20);
            s.setColor(0.95f, 0.30f, 0.75f, 1f);
            s.circle(x + olha, y + 4f * e, 8f * e, 16);
            s.setColor(0.05f, 0.02f, 0.08f, 1f);
            s.circle(x + olha, y + 4f * e, 4f * e, 12);
            s.setColor(1f, 1f, 1f, 1f);
            s.circle(x + olha + 2f * e, y + 7f * e, 1.8f * e, 6);
            if (pisca) {
                s.setColor(0.62f, 0.35f, 0.85f, 1f);
                s.circle(x, y + 4f * e, 14f * e, 20);
            }
            s.setColor(0.15f, 0.05f, 0.22f, 1f);
            s.ellipse(x - 9f * e, y - 15f * e, 18f * e, 7f * e);
        } else {
            s.setColor(0.60f, 0.12f, 0.12f, 1f);
            for (int k = 0; k < 4; k++) {
                float px = x - 15f * e + k * 10f * e;
                float py = y - 12f * e;
                for (int j = 1; j <= 3; j++) {
                    float qx = px + MathUtils.sin(t * 3f + k + j * 0.8f) * 4f * e;
                    float qy = y - 12f * e - j * 8f * e;
                    s.rectLine(px, py, qx, qy, 5f * e * (1f - j * 0.18f));
                    px = qx;
                    py = qy;
                }
            }
            s.setColor(0.85f, 0.22f, 0.22f, 1f);
            s.circle(x, y + 4f * e, 22f * e, 28);
            float[] ox = { -10f, 0f, 10f };
            float[] oy = { 8f, 12f, 8f };
            for (int k = 0; k < 3; k++) {
                s.setColor(1f, 0.92f, 0.3f, 1f);
                s.circle(x + ox[k] * e, y + oy[k] * e, 5f * e, 10);
                s.setColor(0f, 0f, 0f, 1f);
                s.circle(x + ox[k] * e + olha * 0.5f, y + oy[k] * e, pisca ? 0.8f * e : 2.2f * e, 8);
            }
        }
    }

    /** A estrutura termina na plataforma de destino (yCorte): acima dela so existe espaco aberto. */
    private void desenharEstrutura(ShapeRenderer s, float alt, float t, float yCorte) {
        float larguraTotal = EST_X1 - EST_X0;
        float topo = Math.min(Main.ALTURA, yCorte);
        if (topo <= 0f) return;

        int k0 = (int) Math.floor(alt / ALTURA_SEGMENTO);
        float frac = alt - k0 * ALTURA_SEGMENTO;

        // Fundo metalico
        int tm = temaAtual;
        s.setColor(EST_FUNDO[tm][0], EST_FUNDO[tm][1], EST_FUNDO[tm][2], 1f);
        s.rect(EST_X0, 0, larguraTotal, topo);

        for (int j = 0; j <= 7; j++) {
            int indice = k0 + j;
            float y = j * ALTURA_SEGMENTO - frac;
            if (y >= topo) continue;                                   // segmento acima da plataforma

            // Painel do segmento (degrade vertical), cortado na plataforma
            float alturaPainel = Math.min(ALTURA_SEGMENTO - 8f, topo - (y + 8f));
            if (alturaPainel > 0f) {
                s.rect(EST_X0 + 12f, y + 8f, larguraTotal - 24f, alturaPainel,
                       cPainelBaixo, cPainelBaixo, cPainelCima, cPainelCima);
            }

            // Viga horizontal com realce
            if (y + 8f <= topo) {
                s.setColor(EST_VIGA[tm][0], EST_VIGA[tm][1], EST_VIGA[tm][2], 1f);
                s.rect(EST_X0, y, larguraTotal, 8f);
                if (tm == 2) {                                         // fase 3: viga com listras de perigo
                    s.setColor(0.08f, 0.06f, 0.06f, 1f);
                    for (float hx = EST_X0 + 8f; hx < EST_X1; hx += 32f) s.rect(hx, y, 16f, 8f);
                }
                s.setColor(EST_BRILHO[tm][0], EST_BRILHO[tm][1], EST_BRILHO[tm][2], 1f);
                s.rect(EST_X0, y + 6f, larguraTotal, 2f);
                s.setColor(0.95f, 0.97f, 1f, 1f);
                s.circle(EST_X0 + 22f, y + 4f, 3f, 8);
                s.circle(EST_X1 - 22f, y + 4f, 3f, 8);
            }

            // Fase 2 (lunar): crateras no painel, fixas no mundo.
            if (tm == 1 && y + 60f < topo) {
                s.setColor(0.10f, 0.05f, 0.20f, 0.55f);
                for (int q = 0; q < 3; q++) {
                    float cxk = EST_X0 + 70f + ((indice * 97 + q * 211) % 560);
                    float cyk = y + 18f + ((indice * 53 + q * 29) % 34);
                    s.ellipse(cxk - 14f, cyk - 7f, 28f, 14f);
                }
            }

            // Janelas iluminadas (padrao fixo no "mundo", por isso rolam junto com a estrutura)
            if (indice % 3 == 0 && y + 66f < topo) {
                s.setColor(0.04f, 0.05f, 0.11f, 1f);
                Arte.retanguloArredondado(s, 336f, y + 26f, 66f, 40f, 7f);
                s.setColor(0.35f, 0.55f, 0.72f, 1f);
                Arte.retanguloArredondado(s, 341f, y + 31f, 56f, 30f, 5f);
                if (indice % 6 == 0) alien(s, 369f, y + 45f, 0.42f, temaAtual, t, indice);
            }
            if (indice % 4 == 1 && y + 66f < topo) {
                s.setColor(0.04f, 0.05f, 0.11f, 1f);
                Arte.retanguloArredondado(s, 574f, y + 26f, 66f, 40f, 7f);
                s.setColor(0.55f, 0.50f, 0.38f, 1f);
                Arte.retanguloArredondado(s, 579f, y + 31f, 56f, 30f, 5f);
                if (indice % 8 == 1) alien(s, 607f, y + 45f, 0.42f, (temaAtual + 1) % 3, t, indice);
            }

            // Luz de alerta piscando
            if (indice % 5 == 0 && y + 52f < topo) {
                float pisca = 0.35f + 0.55f * MathUtils.sin(t * 4f + indice);
                Arte.brilho(s, 800f, y + 46f, 26f, Arte.COR_ALTO, pisca);
                s.setColor(1f, 0.25f, 0.30f, 0.4f + 0.6f * pisca);
                s.circle(800f, y + 46f, 5f, 12);
            }
        }

        // Degraus pareados na parede: e onde o astronauta pisa (um para cada pe, simetricos em torno dele).
        float dx = 30f * 0.75f;                         // distancia do pe ao eixo, ja na escala do astronauta
        float resto = alt % Astronauta.PASSO_PX;
        for (int j = 0; mostrarDegraus && j < 12; j++) {
            float topoDegrau = Astronauta.Y_SOLA + j * Astronauta.PASSO_PX - resto;
            if (topoDegrau > topo || topoDegrau < -12f) continue;
            for (int k = 0; k < 2; k++) {
                float sg = (k == 0) ? -1f : 1f;
                float x0 = PE_X + sg * dx - 19f;
                s.setColor(0.05f, 0.06f, 0.12f, 0.55f);
                s.rect(x0 - 1f, topoDegrau - 11f, 40f, 11f);
                s.setColor(0.62f, 0.68f, 0.88f, 0.60f);
                s.rect(x0, topoDegrau - 8f, 38f, 8f);
                s.setColor(0.92f, 0.95f, 1f, 0.80f);
                s.rect(x0, topoDegrau - 2f, 38f, 2f);
            }
        }

        // Trilhos de luz nas laterais
        float pulso = 0.55f + 0.25f * MathUtils.sin(t * 2f);
        s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.15f);
        s.rect(EST_X0 + 8f, 0, 14f, topo);
        s.rect(EST_X1 - 22f, 0, 14f, topo);
        s.setColor(cAcento.r, cAcento.g, cAcento.b, pulso);
        s.rect(EST_X0 + 13f, 0, 3f, topo);
        s.rect(EST_X1 - 16f, 0, 3f, topo);

        // Moldura externa
        s.setColor(EST_MOLDURA[tm][0], EST_MOLDURA[tm][1], EST_MOLDURA[tm][2], 1f);
        s.rect(EST_X0 - 6f, 0, 6f, topo);
        s.rect(EST_X1, 0, 6f, topo);
        if (tm == 2) {                                                 // moldura com listras amarelas e pretas
            s.setColor(0.08f, 0.06f, 0.06f, 1f);
            float off = alt % 32f;
            for (float yy = -32f + off; yy < topo; yy += 32f) {
                float h = Math.min(16f, topo - yy);
                if (h <= 0f) continue;
                s.rect(EST_X0 - 6f, Math.max(0f, yy), 6f, yy < 0f ? Math.max(0f, 16f + yy) : h);
                s.rect(EST_X1, Math.max(0f, yy), 6f, yy < 0f ? Math.max(0f, 16f + yy) : h);
            }
        }
    }

    /**
     * Plataforma de "Destino Final": deck com luzes, placa, farol, bandeira e uma construcao diferente por fase
     * (cupula, antena parabolica, portal). y = altura da superficie do deck.
     */
    private void desenharDestino(ShapeRenderer s, float t, int tema, float y) {
        if (y > Main.ALTURA + 330f) return;     // ainda muito acima da tela
        float x0 = EST_X0 - 6f;
        float w = (EST_X1 - EST_X0) + 12f;
        float gx = 660f;                         // centro da construcao principal (lado direito)

        // Construcao principal (atras do deck)
        switch (tema) {
            case 0: // cupula de vidro
                Arte.brilho(s, gx, y + 40f, 170f, cAcento, 0.6f);
                s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.20f);
                s.arc(gx, y, 120f, 0f, 180f, 40);
                s.setColor(0.55f, 0.62f, 0.88f, 1f);
                Arte.arco(s, gx, y, 120f, 6f, 0f, 180f, 30);
                for (int a = 45; a <= 135; a += 45) {
                    float ang = a * MathUtils.PI / 180f;
                    s.rectLine(gx, y, gx + MathUtils.cos(ang) * 120f, y + MathUtils.sin(ang) * 120f, 3f);
                }
                s.setColor(0.95f, 0.85f, 0.5f, 0.85f);
                s.circle(gx - 50f, y + 40f, 6f, 10);
                s.circle(gx + 30f, y + 70f, 5f, 10);
                s.circle(gx + 62f, y + 34f, 6f, 10);
                break;
            case 1: // antena parabolica
                s.setColor(0.55f, 0.62f, 0.88f, 1f);
                s.rectLine(gx, y, gx, y + 100f, 12f);
                s.setColor(0.80f, 0.84f, 0.95f, 1f);
                s.arc(gx, y + 120f, 80f, 205f, 130f, 30);
                s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.9f);
                Arte.arco(s, gx, y + 120f, 80f, 5f, 205f, 335f, 24);
                s.setColor(0.55f, 0.62f, 0.88f, 1f);
                s.rectLine(gx, y + 120f, gx + 34f, y + 176f, 5f);
                float sinal = 0.5f + 0.5f * MathUtils.sin(t * 5f);
                Arte.brilho(s, gx + 34f, y + 178f, 30f, cAcento, sinal);
                s.setColor(1f, 1f, 1f, 0.9f);
                s.circle(gx + 34f, y + 178f, 5f, 10);
                break;
            default: // portal estelar
                float pulsoP = 1f + 0.04f * MathUtils.sin(t * 3f);
                Arte.brilho(s, gx, y + 120f, 190f * pulsoP, cAcento, 1.0f);
                s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.18f);
                s.circle(gx, y + 120f, 92f, 48);
                s.setColor(0.85f, 0.88f, 1f, 1f);
                Arte.anel(s, gx, y + 120f, 100f * pulsoP, 12f, 48);
                s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.55f);
                Arte.anel(s, gx, y + 120f, 76f, 5f, 40);
                Arte.anel(s, gx, y + 120f, 52f + 8f * MathUtils.sin(t * 4f), 3f, 32);
                break;
        }

        // Farol com feixe de luz girando
        float fx = 190f;
        s.setColor(0.55f, 0.62f, 0.88f, 1f);
        s.rectLine(fx, y, fx, y + 64f, 9f);
        float ang = t * 2f;
        s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.14f);
        s.triangle(fx, y + 72f,
                   fx + MathUtils.cos(ang - 0.14f) * 240f, y + 72f + MathUtils.sin(ang - 0.14f) * 240f,
                   fx + MathUtils.cos(ang + 0.14f) * 240f, y + 72f + MathUtils.sin(ang + 0.14f) * 240f);
        float luz = 0.5f + 0.5f * MathUtils.sin(t * 6f);
        Arte.brilho(s, fx, y + 72f, 28f, cAcento, luz);
        s.setColor(1f, 1f, 1f, 0.9f);
        s.circle(fx, y + 72f, 6f, 12);

        // Bandeira
        float bx = 780f;
        s.setColor(0.85f, 0.88f, 1f, 1f);
        s.rectLine(bx, y, bx, y + 124f, 5f);
        s.circle(bx, y + 126f, 5f, 10);
        s.setColor(cAcento.r, cAcento.g, cAcento.b, 1f);
        float onda = MathUtils.sin(t * 3f) * 6f;
        s.triangle(bx, y + 118f, bx, y + 80f, bx + 62f + onda, y + 99f);

        // Placa "Destino Final" (o texto e escrito pela tela de jogo)
        float py0 = y + SINAL_DY;
        s.setColor(0.55f, 0.62f, 0.88f, 1f);
        s.rectLine(SINAL_X - 62f, y, SINAL_X - 62f, py0, 5f);
        s.rectLine(SINAL_X + 62f, y, SINAL_X + 62f, py0, 5f);
        s.setColor(cAcento.r * 0.9f, cAcento.g * 0.9f, cAcento.b * 0.9f, 1f);
        Arte.retanguloArredondado(s, SINAL_X - 96f, py0 - 2f, 192f, SINAL_ALTURA + 4f, 12f);
        s.setColor(0.06f, 0.08f, 0.17f, 1f);
        Arte.retanguloArredondado(s, SINAL_X - 94f, py0, 188f, SINAL_ALTURA, 10f);

        // Deck de pouso
        s.setColor(0.24f, 0.28f, 0.48f, 1f);
        s.rect(x0, y - 36f, w, 36f);
        s.setColor(0.50f, 0.56f, 0.82f, 1f);
        s.rect(x0, y - 4f, w, 4f);
        s.setColor(0.12f, 0.14f, 0.26f, 1f);
        s.rect(x0, y - 36f, w, 5f);
        for (int i = 0; i < 16; i++) {
            float lx = x0 + 22f + i * 44f;
            float a = 0.35f + 0.65f * (0.5f + 0.5f * MathUtils.sin(t * 4f - i * 0.7f));
            s.setColor(cAcento.r, cAcento.g, cAcento.b, a);
            s.circle(lx, y - 18f, 4f, 8);
        }

        // Marca de pouso onde o astronauta chega
        Arte.brilho(s, 480f, y + 2f, 100f, cAcento, 0.6f);
        s.setColor(cAcento.r, cAcento.g, cAcento.b, 0.85f);
        s.ellipse(420f, y - 3f, 120f, 8f);
    }
}
