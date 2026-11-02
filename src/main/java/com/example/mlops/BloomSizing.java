package com.example.mlops;

public class BloomSizing {

    /** m = -n ln(p) / (ln 2)^2, arrondi au supérieur */
    public static int optimalBits(int n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /** k = (m/n) ln 2, arrondi à l'entier le plus proche (minimum 1) */
    public static int optimalHashes(int n, int m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    /** FPR théorique : (1 - e^(-kn/m))^k */
    public static double expectedFpr(int m, int k, int n) {
        return Math.pow(1 - Math.exp(-(double) k * n / m), k);
    }

    public static void main(String[] args) {
        int[] ns = {100, 1_000, 10_000};
        double[] ps = {0.05, 0.01, 0.001};
        String[] names = {"A", "B", "C"};

        for (int i = 0; i < ns.length; i++) {
            int m = optimalBits(ns[i], ps[i]);
            int k = optimalHashes(ns[i], m);
            System.out.printf("Scénario %s : n=%d, p=%.3f -> m=%d, k=%d, FPR prédit=%.4f%%%n",
                    names[i], ns[i], ps[i], m, k, 100 * expectedFpr(m, k, ns[i]));
        }
    }
}