import java.util.*;

/**
 * Algoritmos de planificación de procesos: FIFO, SJF (no apropiativo),
 * PRIORIDAD (no apropiativo, menor número = mayor prioridad) y ROUND ROBIN.
 * Datos de las diapositivas de clase (Wk4).
 */
public class PlanificacionProcesos {

    static final String[] NOMBRES = {"A", "B", "C", "D", "E"};
    // columnas: ráfaga, llegada, prioridad
    static final int[][] DATOS = {
        {3, 2, 2},  // A
        {1, 4, 3},  // B
        {3, 0, 1},  // C
        {4, 1, 3},  // D
        {2, 3, 4}   // E
    };
    static final int QUANTUM = 3;
    // Como en las diapositivas: retorno = instante en que termina el proceso.
    // Si tu profesor usa la definición clásica (fin - llegada), cambia a true.
    static final boolean RETORNO_CLASICO = false;

    static final int RAFAGA = 0, LLEGADA = 1, PRIORIDAD = 2;

    // Cada tramo del Gantt: {proceso, inicio, fin}
    interface Criterio { int valor(int p); }

    /** Planificador no apropiativo genérico. Desempate: el que llegó antes. */
    static List<int[]> noApropiativo(Criterio criterio) {
        int n = DATOS.length;
        boolean[] hecho = new boolean[n];
        List<int[]> gantt = new ArrayList<>();
        int t = 0, terminados = 0;
        while (terminados < n) {
            int elegido = -1;
            for (int p = 0; p < n; p++) {
                if (hecho[p] || DATOS[p][LLEGADA] > t) continue;
                if (elegido == -1
                        || criterio.valor(p) < criterio.valor(elegido)
                        || (criterio.valor(p) == criterio.valor(elegido)
                            && DATOS[p][LLEGADA] < DATOS[elegido][LLEGADA])) {
                    elegido = p;
                }
            }
            if (elegido == -1) {           // CPU ociosa: saltar a la próxima llegada
                int proxima = Integer.MAX_VALUE;
                for (int p = 0; p < n; p++)
                    if (!hecho[p]) proxima = Math.min(proxima, DATOS[p][LLEGADA]);
                t = proxima;
                continue;
            }
            gantt.add(new int[]{elegido, t, t + DATOS[elegido][RAFAGA]});
            t += DATOS[elegido][RAFAGA];
            hecho[elegido] = true;
            terminados++;
        }
        return gantt;
    }

    static List<int[]> fifo()      { return noApropiativo(p -> DATOS[p][LLEGADA]); }
    static List<int[]> sjf()       { return noApropiativo(p -> DATOS[p][RAFAGA]); }
    static List<int[]> prioridad() { return noApropiativo(p -> DATOS[p][PRIORIDAD]); }

    static List<int[]> roundRobin(int q) {
        int n = DATOS.length;
        int[] restante = new int[n];
        for (int p = 0; p < n; p++) restante[p] = DATOS[p][RAFAGA];

        // procesos ordenados por llegada
        List<Integer> porLlegar = new ArrayList<>();
        for (int p = 0; p < n; p++) porLlegar.add(p);
        porLlegar.sort(Comparator.comparingInt(p -> DATOS[p][LLEGADA]));

        Deque<Integer> cola = new ArrayDeque<>();
        List<int[]> gantt = new ArrayList<>();
        int t = 0;
        admitir(porLlegar, cola, t);
        while (!cola.isEmpty() || !porLlegar.isEmpty()) {
            if (cola.isEmpty()) {
                t = DATOS[porLlegar.get(0)][LLEGADA];
                admitir(porLlegar, cola, t);
            }
            int p = cola.pollFirst();
            int ejec = Math.min(q, restante[p]);
            gantt.add(new int[]{p, t, t + ejec});
            t += ejec;
            restante[p] -= ejec;
            admitir(porLlegar, cola, t);   // primero entran los que llegaron...
            if (restante[p] > 0) cola.addLast(p); // ...y luego el interrumpido
        }
        return gantt;
    }

    static void admitir(List<Integer> porLlegar, Deque<Integer> cola, int hasta) {
        while (!porLlegar.isEmpty() && DATOS[porLlegar.get(0)][LLEGADA] <= hasta)
            cola.addLast(porLlegar.remove(0));
    }

    /** Muestra Gantt y tiempos; devuelve {TEM, TRM}. */
    static double[] mostrar(String titulo, List<int[]> gantt) {
        int n = DATOS.length;
        int[] fin = new int[n];
        for (int[] g : gantt) fin[g[0]] = g[2];

        System.out.println("========================================================");
        System.out.println(titulo);
        System.out.println("========================================================");
        StringBuilder sb = new StringBuilder("Gantt: ");
        for (int[] g : gantt)
            sb.append("|").append(NOMBRES[g[0]]).append(" (")
              .append(g[1]).append("-").append(g[2]).append(") ");
        System.out.println(sb.append("|"));

        System.out.printf("%-6s%-12s%-12s%n", "Proc", "T. espera", "T. retorno");
        double sumaEspera = 0, sumaRetorno = 0;
        for (int p = 0; p < n; p++) {
            int espera = fin[p] - DATOS[p][LLEGADA] - DATOS[p][RAFAGA];
            int retorno = RETORNO_CLASICO ? fin[p] - DATOS[p][LLEGADA] : fin[p];
            sumaEspera += espera;
            sumaRetorno += retorno;
            System.out.printf("%-6s%-12d%-12d%n", NOMBRES[p], espera, retorno);
        }
        double tem = sumaEspera / n, trm = sumaRetorno / n;
        System.out.printf(Locale.US, "TEM (espera medio)  = %.1f ut%n", tem);
        System.out.printf(Locale.US, "TRM (retorno medio) = %.1f ut%n%n", trm);
        return new double[]{tem, trm};
    }

    public static void main(String[] args) {
        String[] algoritmos = {"FIFO", "SJF", "PRIORIDAD", "ROUND ROBIN"};
        double[][] resumen = new double[4][];
        resumen[0] = mostrar("FIFO", fifo());
        resumen[1] = mostrar("SJF (no apropiativo)", sjf());
        resumen[2] = mostrar("PRIORIDAD (no apropiativo)", prioridad());
        resumen[3] = mostrar("ROUND ROBIN (q=" + QUANTUM + " ut)", roundRobin(QUANTUM));

        System.out.println("========================================================");
        System.out.println("TABLA RESUMEN");
        System.out.println("========================================================");
        System.out.printf("%-14s%-18s%-16s%n", "Algoritmos", "T media ESPERA", "T media RETORNO");
        int mejor = 0;
        for (int i = 0; i < 4; i++) {
            System.out.printf(Locale.US, "%-14s%-18.1f%-16.1f%n", algoritmos[i], resumen[i][0], resumen[i][1]);
            if (resumen[i][0] < resumen[mejor][0]) mejor = i;
        }
        System.out.printf(Locale.US, "%nMejor algoritmo (menor tiempo medio de espera): %s con TEM = %.1f ut%n",
                algoritmos[mejor], resumen[mejor][0]);
    }
}