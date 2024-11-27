package examples.algoritmoGenetico;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.OneShotBehaviour;
import org.json.JSONObject;
import org.json.JSONArray;

public class BuscaServicio extends Agent {

    public class DataSet {
        protected double[] x;
        protected double[] x1;
        protected double[] x2;
        protected double[] y;

        // Constructor para regresión simple/polinomial
        public DataSet(double[] x, double[] y) {
            this.x = x;
            this.y = y;
        }

        // Constructor para regresión múltiple
        public DataSet(double[] x1, double[] x2, double[] y) {
            this.x1 = x1;
            this.x2 = x2;
            this.y = y;
        }
    }

    public class regresionLinealSimple {

        public void predicciones(double[] coef, double[] nuevosx) {
            for (double x : nuevosx) {
                double res = (coef[0] + coef[1] * x);
                System.out.println("y = " + coef[0] + " + " + coef[1] + " * (" + x + ") = " + res);
            }
        }
    }

    public class regresionLinealMultiple {

        public void predicciones(double[] coef, double[] nuevosVX1, double[] nuevosVX2) {
            for (int i = 0; i < 5; i += 1) {
                double b = nuevosVX1[i];
                double c = nuevosVX2[i];
                double res = (coef[0] + coef[1] * b + coef[2] * c);
                System.out.println("y = " + coef[0] + " + " + coef[1] + " ( " + b + " ) + "
                        + coef[2] + " ( " + c + " ) = " + res);

            }
        }
    }

    public class regresionPolinomial {
        public void predicciones(double[] coef, double[] nuevosX) {
            for (double x : nuevosX) {
                double prediccion = coef[0] + coef[1] * x + coef[2] * Math.pow(x, 2);
                System.out.println("y = " + coef[0] + " + " + coef[1] + "* ( " + x + " ) +" +
                        coef[2] + "* ( " + x + " )^{2} = " + prediccion);
            }
        }
    }

    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");
        doWait(5000);
        addBehaviour(new ClasificacionYRegresionBehaviour());
    }

    private class ClasificacionYRegresionBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            // Definir el conjunto de datos
            DataSet data = new DataSet(
                    new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9}, // x o x1 Para regresiones simples y polinomiales
                    new double[] { }, // x2 (solo para regresión múltiple)
                    new double[] {3, 6, 9, 12, 15, 18, 21, 24, 27} // y
            );

            double[] nuevosx1 = { 43, 48, 52, 57, 58};
            double[] nuevosx2 = { 32.9, 33.3, 29.9, 34.5, 30.8 };

            // Buscar agentes de clasificación
            AID classificationAgent = searchAgent("servicio-clasificacion");
            if (classificationAgent == null) {
                System.out.println("No se encontró un agente de clasificación.");
                return;
            }

            // Enviar solicitud de clasificación
            
            ACLMessage classMsg = new ACLMessage(ACLMessage.REQUEST);
            classMsg.addReceiver(classificationAgent);
            classMsg.setContent(serializarVariablesJson(data));
            classMsg.setConversationId("servicio-clasificacion");
            send(classMsg);

            // Recibir recomendación de análisis
            
            ACLMessage classReply = blockingReceive();
            if (classReply != null && classReply.getPerformative() == ACLMessage.INFORM) {
                String tipoDeAnalisis = classReply.getContent();
                System.out.println("Tipo de análisis recomendado: " + tipoDeAnalisis);

                // Determinar el tipo de servicio de regresión
                String regresionRecomendada = "";
                switch (tipoDeAnalisis) {
                    case "Regresion Lineal Simple":
                        regresionRecomendada = "servicio-regresion.simple";
                        break;
                    case "Regresion Lineal Multiple":
                        regresionRecomendada = "servicio-regresion-multiple";
                        break;
                    case "Regresion Polinomial":
                        regresionRecomendada = "servicio-regresion-polinomial";
                        break;
                    default:
                        System.out.println("Tipo de análisis desconocido.");
                        return;
                }

                // Buscar el agente de regresión adecuado y enviar los datos
                AID agenteRegresion = searchAgent(regresionRecomendada);
                if (agenteRegresion != null) {
                    ACLMessage regressionRequest = new ACLMessage(ACLMessage.REQUEST);
                    regressionRequest.addReceiver(agenteRegresion);
                    regressionRequest.setContent(serializarVariablesJson(data));
                    regressionRequest.setConversationId("analisis-regresion");
                    send(regressionRequest);

                    // Recibir parámetros de regresión
                    ACLMessage regressionReply = blockingReceive();
                    if (regressionReply != null && regressionReply.getPerformative() == ACLMessage.INFORM) {
                        System.out.println("Parámetros de regresión recibidos: " + regressionReply.getContent());

                        String jsonContent = regressionReply.getContent();
                        double[] coeficientes = extraerCoeficientesJson(jsonContent);

                        // Ejemplo de datos nuevos para realizar predicciones
                        

                        switch (tipoDeAnalisis) {
                            case "Regresion Lineal Simple":
                                regresionLinealSimple regresion = new regresionLinealSimple();
                                regresion.predicciones(coeficientes, nuevosx1); 
                                break;
                            case "Regresion Lineal Multiple":
                                regresionLinealMultiple regresionM = new regresionLinealMultiple();
                                regresionM.predicciones(coeficientes, nuevosx1, nuevosx2);
                                break;
                            case "Regresion Polinomial":
                                regresionPolinomial regresionP = new regresionPolinomial();
                                regresionP.predicciones(coeficientes, nuevosx1);
                                break;

                            default:
                                break;
                        }

                    }
                } else {
                    System.out.println("No se encontró el agente de regresión adecuado.");
                }
            }

                        AID agenteGenetico = searchAgent("servicio-algoritmo-genetico");
                         if (agenteGenetico == null) {
                        System.out.println("No se encontró un agente de Agente genetico.");
                        return;
                        }
                         // Enviar solicitud de agente genetico
                        
                         ACLMessage geneticRequest = new ACLMessage(ACLMessage.REQUEST);
                         geneticRequest.addReceiver(agenteGenetico);
                         geneticRequest.setContent(serializarVariablesJson(data));
                         geneticRequest.setConversationId("analisis-genetico");
                         send(geneticRequest);

                            // Recibir coeficientes optimizados
                            ACLMessage geneticReply = blockingReceive();
                            if (geneticReply != null && geneticReply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("Parámetros de recibidos: " + geneticReply.getContent());
                            String jsonContent = geneticReply.getContent();
                            double[] coefOptimos = extraerCoeficientesJson(jsonContent);
                            coefOptimos = extraerCoeficientesJson(geneticReply.getContent());
                            regresionLinealSimple regresion = new regresionLinealSimple();

                            // Realizar predicciones con coeficientes optimizados
                            regresion.predicciones(coefOptimos, nuevosx1);
                            System.out.println(coefOptimos[2]);
                            } else {
                               System.out.println("No se recibió respuesta del agente de algoritmo genético.");
                            }




        }

        // Busca agentes y devuelve AID del primer agente encontrado
        private AID searchAgent(String serviceType) {
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
    sd.setType(serviceType);
    template.addServices(sd);

    try {
        DFAgentDescription[] results = DFService.search(myAgent, template);
        if (results.length > 0) {
            System.out.println("Agente encontrado para el servicio: " + serviceType);
            return results[0].getName(); // Devuelve el AID del agente encontrado
             }else {
            System.out.println("No se encontraron agentes para el servicio: " + serviceType);
             }
             } catch (FIPAException e) {
                 e.printStackTrace();
             }
         return null;
            }
    }

    // Transformar el arreglo obtenido del agente de regresion a un arreglo
    private double[] extraerCoeficientesJson(String jsonContent) {
        JSONObject jsonObject = new JSONObject(jsonContent);
        JSONArray coeficientesArray = jsonObject.getJSONArray("Coeficientes");
        double[] coeficientes = new double[coeficientesArray.length()];

        for (int i = 0; i < coeficientesArray.length(); i++) {
            coeficientes[i] = coeficientesArray.getDouble(i);
        }
        return coeficientes;
    }

    private String serializarVariablesJson(DataSet data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (data.x != null) {
            sb.append("\"x\": [");
            for (int i = 0; i < data.x.length; i++) {
                sb.append(data.x[i]);
                if (i < data.x.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        if (data.x1 != null) {
            sb.append("\"x1\": [");
            for (int i = 0; i < data.x1.length; i++) {
                sb.append(data.x1[i]);
                if (i < data.x1.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        if (data.x2 != null) {
            sb.append("\"x2\": [");
            for (int i = 0; i < data.x2.length; i++) {
                sb.append(data.x2[i]);
                if (i < data.x2.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        sb.append("\"y\": [");
        for (int i = 0; i < data.y.length; i++) {
            sb.append(data.y[i]);
            if (i < data.y.length - 1)
                sb.append(", ");
        }
        sb.append("] }"); // Asegura que el JSON termine con "}"

        return sb.toString();
    }

}
