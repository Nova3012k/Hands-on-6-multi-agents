package examples.algoritmoGenetico;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Arrays;

public class OfreceServicio extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");

        // Registrar el servicio de clasificación una sola vez
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("servicio-clasificacion");
        sd.setName("servicio-clasificacion");
        dfd.addServices(sd);
        try {
            // registra el agente
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Añadir comportamiento para recibir mensajes de otros agentes
        addBehaviour(new OfreceClasificacionBehaviour());
    }

    @Override
    // Desregistra al agente del DF y notifica que ya no ofrece sus servicios.
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("El agente " + getAID().getName() + " ya no ofrece sus servicios.");
    }

    // El comportamiento cíclico permite al agente recibir mensajes continuamente y
    // determinar el tipo de regresión adecuado.
    private class OfreceClasificacionBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            // recibe el mensaje y determina si no es nulo
            if (msg != null && msg.getConversationId().equals("servicio-clasificacion")) {

                // Extrae el arreglo en formato JSON
                JSONObject json = new JSONObject(msg.getContent());
                JSONArray yArray = json.getJSONArray("y");
                double[] y = parseJsonArray(yArray);

                double[] x1 = null;
                double[] x2 = null;

                // Verificación de regresión múltiple o simple/polinómica
                // Y extrae los datos de los arreglos de x
                if (json.has("x1") && json.has("x2") && json.getJSONArray("x2").length() > 0) {
                    x1 = parseJsonArray(json.getJSONArray("x1"));
                    x2 = parseJsonArray(json.getJSONArray("x2"));
                } else if (json.has("x1")) {
                    x1 = parseJsonArray(json.getJSONArray("x1"));
                }

                // Determinar tipo de regresión
                String regresionRecomendada = clasificacionDeRegresion(x1, x2, y);

                // Enviar tipo de análisis recomendado
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(regresionRecomendada);
                send(reply);
                System.out.println("Análisis recomendado enviado: " + regresionRecomendada);

            } else {
                block(); // Block until new messages arrive
            }
        }

        // convierte un JSONArray (arreglo en formato JSON) a un arreglo de double en
        // Java
        private double[] parseJsonArray(JSONArray jsonArray) {
            double[] array = new double[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                array[i] = jsonArray.getDouble(i);
            }
            return array;
        }
    }

    private String clasificacionDeRegresion(double[] x1, double[] x2, double[] y) {
    if (x2 != null && x2.length > 0) {
        // Regresión múltiple
        double correlacionX1 = calcularCoorrelacion(x1, y);
        double correlacionX2 = calcularCoorrelacion(x2, y);

        if (Math.abs(correlacionX1) > 0.7 && Math.abs(correlacionX2) > 0.7) {
            return "Regresion Lineal Multiple";
        }
    } else if (x1 != null) {
        // Evaluar modelos de regresión lineal y polinomial
        double r2Lineal = calcularR2(x1, y, 1); // Grado 1
        double r2Polinomial = calcularR2(x1, y, 2); // Grado 2
        double mseLineal = calcularMSE(x1, y, 1); // Error cuadrático medio grado 1
        double msePolinomial = calcularMSE(x1, y, 2); // Error cuadrático medio grado 2

        // Imprimir métricas para diagnóstico
        System.out.println("R^2 Lineal: " + r2Lineal);
        System.out.println("R^2 Polinomial: " + r2Polinomial);
        System.out.println("MSE Lineal: " + mseLineal);
        System.out.println("MSE Polinomial: " + msePolinomial);

        // Selección basada en métricas
        double Umbral = 0.02; // Umbral para mejora significativa
        if ((r2Polinomial - r2Lineal > Umbral) && (msePolinomial < mseLineal)) {
            return "Regresion Polinomial";
        } else if (r2Lineal > 0.85) {
            return "Regresion Lineal Simple";
        }
    }
    return "Tipo de análisis desconocido.";
}

private double calcularMSE(double[] x, double[] y, int grado) {
    double[] coeficientes = ajustarRegresion(x, y, grado);
    double mse = 0.0;

    for (int i = 0; i < x.length; i++) {
        double prediccion = coeficientes[0];
        for (int j = 1; j <= grado; j++) {
            prediccion += coeficientes[j] * Math.pow(x[i], j);
        }
        mse += Math.pow(y[i] - prediccion, 2);
    }
    return mse / x.length; // Promedio del error cuadrático
}


    private double calcularR2(double[] x, double[] y, int grado) {
    // Ajustar modelo de regresión
    double[] coeficientes = ajustarRegresion(x, y, grado);
    double sumaTotal = 0.0;
    double sumaResiduos = 0.0;
    double promedioY = Arrays.stream(y).average().orElse(0);

    for (int i = 0; i < x.length; i++) {
        // Calcular predicción
        double prediccion = coeficientes[0];
        for (int j = 1; j <= grado; j++) {
            prediccion += coeficientes[j] * Math.pow(x[i], j);
        }
        sumaTotal += Math.pow(y[i] - promedioY, 2);
        sumaResiduos += Math.pow(y[i] - prediccion, 2);
     }

    // Calcular R^2
    return 1 - (sumaResiduos / sumaTotal);
    }

    private double[] ajustarRegresion(double[] x, double[] y, int grado) {
        int n = x.length;
        double[][] matriz = new double[n][grado + 1];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= grado; j++) {
                matriz[i][j] = Math.pow(x[i], j);
            }
        }

        double[][] XtX = multiplicarMatrices(transponerMatriz(matriz), matriz);
        double[] XtY = multiplicarMatrizVector(transponerMatriz(matriz), y);
        double[][] XtXInversa = calcularInversa(XtX);

        return multiplicarMatrizVector(XtXInversa, XtY);
    }

     public double[][] calcularInversa(double[][] matriz) {
    int n = matriz.length;

    if (n == 2) {
        // Calcular inversa para una matriz 2x2
        double det = matriz[0][0] * matriz[1][1] - matriz[0][1] * matriz[1][0];

        double[][] inversa = new double[2][2];
        inversa[0][0] = matriz[1][1] / det;
        inversa[0][1] = -matriz[0][1] / det;
        inversa[1][0] = -matriz[1][0] / det;
        inversa[1][1] = matriz[0][0] / det;

        return inversa;

    } else if (n == 3) {
        // Calcular inversa para una matriz 3x3
        double det = matriz[0][0] * (matriz[1][1] * matriz[2][2] - matriz[1][2] * matriz[2][1])
                - matriz[0][1] * (matriz[1][0] * matriz[2][2] - matriz[1][2] * matriz[2][0])
                + matriz[0][2] * (matriz[1][0] * matriz[2][1] - matriz[1][1] * matriz[2][0]);

        double[][] inversa = new double[3][3];
        inversa[0][0] = (matriz[1][1] * matriz[2][2] - matriz[1][2] * matriz[2][1]) / det;
        inversa[0][1] = (matriz[0][2] * matriz[2][1] - matriz[0][1] * matriz[2][2]) / det;
        inversa[0][2] = (matriz[0][1] * matriz[1][2] - matriz[0][2] * matriz[1][1]) / det;
        inversa[1][0] = (matriz[1][2] * matriz[2][0] - matriz[1][0] * matriz[2][2]) / det;
        inversa[1][1] = (matriz[0][0] * matriz[2][2] - matriz[0][2] * matriz[2][0]) / det;
        inversa[1][2] = (matriz[0][2] * matriz[1][0] - matriz[0][0] * matriz[1][2]) / det;
        inversa[2][0] = (matriz[1][0] * matriz[2][1] - matriz[1][1] * matriz[2][0]) / det;
        inversa[2][1] = (matriz[0][1] * matriz[2][0] - matriz[0][0] * matriz[2][1]) / det;
        inversa[2][2] = (matriz[0][0] * matriz[1][1] - matriz[0][1] * matriz[1][0]) / det;

        return inversa;
    } else {
        throw new IllegalArgumentException("Solo se admiten matrices de tamaño 2x2 o 3x3.");
    }
}

     private double[][] transponerMatriz(double[][] matriz) {
        int filas = matriz.length;
        int columnas = matriz[0].length;
        double[][] transpuesta = new double[columnas][filas];
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                transpuesta[j][i] = matriz[i][j];
            }
        }
        return transpuesta;
    }

    private double[][] multiplicarMatrices(double[][] A, double[][] B) {
    // Verificar dimensiones compatibles
    int filasA = A.length;
    int columnasA = A[0].length;
    int filasB = B.length;
    int columnasB = B[0].length;

    // Inicializar matriz de resultado
    double[][] resultado = new double[filasA][columnasB];

    // Realizar la multiplicación
    for (int i = 0; i < filasA; i++) {
        for (int j = 0; j < columnasB; j++) {
            for (int k = 0; k < columnasA; k++) {
                resultado[i][j] += A[i][k] * B[k][j];
            }
        }
    }

    return resultado;
}

private double[] multiplicarMatrizVector(double[][] matriz, double[] vector) {
        int filas = matriz.length;
        int columnas = matriz[0].length;
        double[] resultado = new double[filas];

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                resultado[i] += matriz[i][j] * vector[j];
            }
        }

        return resultado;
    }

    private double calcularCoorrelacion(double[] x, double[] y) {
        // calcula el promedio de los elementos x y y
        double promedioX = Arrays.stream(x).average().orElse(0);
        double promedioY = Arrays.stream(y).average().orElse(0);

        double numerador = 0.0;
        double denominadorX = 0.0;
        double denominadorY = 0.0;

        // Realiza las sumatorias respectivas
        for (int i = 0; i < x.length; i++) {
            numerador += (x[i] - promedioX) * (y[i] - promedioY);
            denominadorX += Math.pow(x[i] - promedioX, 2);
            denominadorY += Math.pow(y[i] - promedioY, 2);
        }

        // Evitar la división por cero
        if (denominadorX == 0 || denominadorY == 0) {
            return 0;
        }

        return numerador / Math.sqrt(denominadorX * denominadorY);
    }
    
}
