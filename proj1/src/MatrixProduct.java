import java.util.Scanner;

public class MatrixProduct{

    public static void OnMult(int m_ar, int m_br) {

        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];

        for (int i = 0; i < m_ar * m_ar; i++) {
            pha[i] = 1.0;
            phb[i] = 0;
            phc[i] = 0;
        }

        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                double temp = 0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        System.out.println("Time: " + String.format("%.3f", executionTime) + " seconds");

        // display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
            System.out.println();
        }
    }

    public static void OnMultLine(int m_ar, int m_br) {

        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];

        for (int i = 0; i < m_ar * m_ar; i++) {
            pha[i] = 1.0;
            phb[i] = 0;
            phc[i] = 0;
        }

        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }

        long startTime = System.currentTimeMillis();

        // perform element-wise multiplication
        for (int i = 0; i < m_ar; i++) {
            for (int k = 0; k < m_ar; k++) {
                for (int j = 0; j < m_br; j++) {
                    phc[i * m_br + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                }
            }
        }

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        System.out.println("Time: " + String.format("%.3f", executionTime) + " seconds");

        // display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_ar); j++) {
                System.out.print(phc[j] + " ");
            }
            System.out.println();
        }
    }

    public static void OnMultBlock(int m_ar, int m_br, int bkSize) {

        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];

        for (int i = 0; i < m_ar * m_ar; i++) {
            pha[i] = 1.0;
            phb[i] = 0;
            phc[i] = 0;
        }

        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }

        long startTime = System.currentTimeMillis();

        // perform block-wise multiplication
        for (int ii = 0; ii < m_ar; ii += bkSize) {
            for (int jj = 0; jj < m_br; jj += bkSize) {
                for (int kk = 0; kk < m_ar; kk += bkSize) {
                    for (int i = ii; i < Math.min(ii + bkSize, m_ar); i++) {
                        for (int k = kk; k < Math.min(kk + bkSize, m_ar); k++) {
                            for (int j = jj; j < Math.min(jj + bkSize, m_br); j++) {
                                phc[i * m_br + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                            }
                        }
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        System.out.println("Time: " + String.format("%.3f", executionTime) + " seconds");

        // display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_ar); j++) {
                System.out.print(phc[j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("1. Multiplication\n");
        System.out.println("2. Line Multiplication\n");
        System.out.println("3. Block Multiplication\n");
        System.out.print("Selection?: ");
        int selection = scanner.nextInt();
        System.out.print("Dimensions: lins=cols ? ");
        int dimensions = scanner.nextInt();
        if (selection == 1) {
            OnMult(dimensions, dimensions);
        } else if (selection == 2) {
            OnMultLine(dimensions, dimensions);
        } else if (selection == 3) {
            System.out.print("Choose a block size: ");
            int blkSize = scanner.nextInt();
            OnMultBlock(dimensions, dimensions, blkSize);
        }

        scanner.close();
    }
}
