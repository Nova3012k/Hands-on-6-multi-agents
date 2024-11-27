package examples.algoritmoGenetico;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;

import java.util.Arrays;

import org.json.JSONArray;

public class RegresionSimpleAgent extends Agent {

    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");

        // Registrar el servicio de Clasificacion-analisis
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        // Establece el tipo del servicio
        sd.setType("servicio-regresion.simple");
        // Asigna un nombre al servicio
        sd.setName("servicio-regresion.simple");
        dfd.addServices(sd);
        try {
            // registra el agente
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ReceiveDatasetBehaviour());
    }

    // convierte un JSONArray (arreglo en formato JSON) a un arreglo de double en
    // Java
    public double[] parseJsonArray(JSONArray jsonArray) {
        double[] array = new double[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            array[i] = jsonArray.getDouble(i);
        }
        return array;
    }

    private String calcularRegresionSimple(double[] x, double[] y) {
        matematicasDiscretas discretas = new matematicasDiscretas();
        algebraLineal algebra = new algebraLineal();

        double sumx = discretas.sumatoria(x);
        double sumy = discretas.sumatoria(y);
        double sumx2 = discretas.sumatoriaExponencial(x);
        double sumxy = discretas.sumatoriadeProductos(x, y);
        double sumy2 = discretas.sumatoriaExponencial(y);

        double b1 = algebra.generarB1(sumxy, y, sumx, sumy, sumx2);
        double b0 = algebra.generarB0(sumy, b1, sumx, y);

        double[] coeficientes = { b0, b1 };

        JSONObject resultado = new JSONObject();
        resultado.put("Coeficientes", coeficientes);

        return resultado.toString();
    }

    public class algebraLineal {
        public double generarB1(double sumxy, double[] y, double sumx, double sumy, double sumx2) {
            return (sumxy - (sumx * sumy) / y.length) / (sumx2 - Math.pow(sumx, 2) / y.length);
        }

        public double generarB0(double sumy, double b1, double sumx, double[] y) {
            return (sumy / y.length) - b1 * (sumx / y.length);
        }

        public double coeficienteCorrelacion(double x[], double sumxy, double sumx, double sumy, double sumx2,
                double sumy2) {
            return (x.length * sumxy - sumx * sumy)
                    / Math.sqrt((x.length * sumx2 - sumx * sumx) * (x.length * sumy2 - sumy * sumy));
        }

        public double coeficienteDeterminacion(double cc) {
            return Math.pow(cc, 2) * 100;
        }
    }

    public class matematicasDiscretas {
        public double sumatoria(double[] a) {
            return Arrays.stream(a).sum();
        }

        public double sumatoriaExponencial(double[] a) {
            return Arrays.stream(a).map(num -> Math.pow(num, 2)).sum();
        }

        public double sumatoriadeProductos(double[] a, double[] b) {
            double sum = 0;
            for (int i = 0; i < a.length; i++) {
                sum += a[i] * b[i];
            }
            return sum;
        }

    }

    private class ReceiveDatasetBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            // método que intenta recibir un mensaje.
            ACLMessage msg = receive();
            // Asegura que efectivamente se ha recibido un mensaje
            // Y que es parte de una conversación de análisis de regresión
            if (msg != null && msg.getConversationId().equals("analisis-regresion")) {
                JSONObject json = new JSONObject(msg.getContent());

                JSONArray xArray = json.getJSONArray("x1");
                JSONArray yArray = json.getJSONArray("y");

                double[] x = parseJsonArray(xArray);
                double[] y = parseJsonArray(yArray);

                // Realizar regresión y obtener los resultados
                String resultado = calcularRegresionSimple(x, y);

                // Enviar los resultados al agente solicitante
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(resultado);
                send(reply);
                System.out.println("Resultados de regresión simple enviados al agente solicitante.");
            } else {
                block();
            }
        }
    }

}
