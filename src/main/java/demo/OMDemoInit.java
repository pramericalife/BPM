package demo;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.ProposalInfo;
import com.example.SupplierInfo;
import com.thoughtworks.xstream.XStream;

import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.api.task.model.Task;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
/**
 * OMDemoInit
 */
@SuppressWarnings("unchecked")
public class OMDemoInit {

    final static private int PROBABILITY = 60;
    private static final String userId = "Administrator";
    private static String processId = "Online-Journey.online-journey";
    private static Random random = new Random(System.currentTimeMillis());
    private static String apiUrl="https://api.pramericalife.in/api/wrapper/cibil-score?proposalNo=AF002535410";

    public static void startProcesses(ProcessContext kcontext) {
        String deploymentId = (String) kcontext.getKieRuntime().getEnvironment().get("deploymentId");

        ProcessService processService = (ProcessService) ServiceRegistry.get().service(ServiceRegistry.PROCESS_SERVICE);

        InputStream res = OMDemoInit.class.getClassLoader().getResourceAsStream("demo/proposal-info-list.xml");
        XStream xstream = new XStream();
        xstream.setClassLoader(OMDemoInit.class.getClassLoader());
        xstream.allowTypesByWildcard(new String[] { 
            "com.example.**"
        });

        Collection<ProposalInfo> list = (Collection<ProposalInfo>) xstream.fromXML(res);

        Map<String, Object> params = new HashMap<>();
        List<Long> processInstanceList = new ArrayList<>(list.size());

        for (ProposalInfo orderInfo : list) {
            params.clear();
            params.put("orderInfo", orderInfo);

            Long processInstanceId = processService.startProcess(deploymentId, processId, params);

            processInstanceList.add(processInstanceId);
        }

        kcontext.setVariable("processInstanceList", processInstanceList);
    }

    public static void performTasksRequestOffer(ProcessContext kcontext) {
        RuntimeDataService runtimeDataService = (RuntimeDataService) ServiceRegistry.get()
                .service(ServiceRegistry.RUNTIME_DATA_SERVICE);

        List<Long> processInstanceList = (List<Long>) kcontext.getVariable("processInstanceList");

        List<Long> piUpdated = new ArrayList<>();
        for (Long id : processInstanceList) {
            if (random.nextInt(100) < PROBABILITY) {
                piUpdated.add(id);
                List<Long> taskIdList = runtimeDataService.getTasksByProcessInstanceId(id);

                for (Long taskId : taskIdList) {
                    UserTaskService userTaskService = (UserTaskService) ServiceRegistry.get()
                            .service(ServiceRegistry.USER_TASK_SERVICE);

                    Map<String, Object> inputParams = userTaskService.getTaskInputContentByTaskId(taskId);
                    ProposalInfo orderInfo = (ProposalInfo) inputParams.get("orderInfo");
                    orderInfo.setTargetPremium(60 * random.nextInt(10) + 110);
                    orderInfo.setCategory(random.nextBoolean() ? "basic" : "optional");
                    List<String> suppliers;
                    if (random.nextInt(1) == 0)
                        suppliers = new ArrayList<>(Arrays.asList("supplier1", "supplier3"));
                    else
                        suppliers = new ArrayList<>(Arrays.asList("supplier2", "supplier3"));

                    orderInfo.setSuppliersList(suppliers);

                    Map<String, Object> outputParams = new HashMap<>();
                    outputParams.put("orderInfo", orderInfo);
                    userTaskService.completeAutoProgress(taskId, userId, outputParams);
                }
            }
        }
        kcontext.setVariable("processInstanceList", piUpdated);
    }

    public static void performTasksPrepareOffer(ProcessContext kcontext) {
        RuntimeDataService runtimeDataService = (RuntimeDataService) ServiceRegistry.get()
                .service(ServiceRegistry.RUNTIME_DATA_SERVICE);

        UserTaskService userTaskService = (UserTaskService) ServiceRegistry.get()
                .service(ServiceRegistry.USER_TASK_SERVICE);

        List<Long> processInstanceList = (List<Long>) kcontext.getVariable("processInstanceList");

        List<Long> piUpdated = new ArrayList<>();

        for (Long id : processInstanceList) {
            if (random.nextInt(100) < PROBABILITY) {
                piUpdated.add(id);
                List<Long> taskIdList = runtimeDataService.getTasksByProcessInstanceId(id);

                for (Long taskId : taskIdList) {
                    Task task = userTaskService.getTask(taskId);

                    if (task.getName().contentEquals("Prepare Offer")) {
                        Map<String, Object> iomap = userTaskService.getTaskInputContentByTaskId(taskId);
                        ProposalInfo orderInfo = (ProposalInfo) iomap.get("orderInfo");
                        SupplierInfo supplierInfo = new SupplierInfo();
                        supplierInfo.setDeliveryDate(new Date(
                                LocalDateTime.now().plusDays(random.nextInt(15)).toEpochSecond(ZoneOffset.UTC)));
                        supplierInfo.setOffer(orderInfo.getTargetPremium() + 10 * random.nextInt(10));
                        supplierInfo.setUser((String) iomap.get("supplier"));
                        iomap.put("supplierInfoOut", supplierInfo);
                        userTaskService.completeAutoProgress(taskId, userId, iomap);
                    }
                }
            }
        }
        kcontext.setVariable("processInstanceList", piUpdated);
    }

    public static void testApijBPM() {
        try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // XStream xStream = new XStream();
        // xStream.setClassLoader(OMDemoInit.class.getClassLoader());
        // List<PerformTask> performTasks = new ArrayList<>();
        // performTasks.add(task);
        // System.out.println(xStream.toXML(performTasks));

        // BeanUtilsBean util = new BeanUtilsBean() {
        // @Override
        // public void copyProperty(Object obj, String name, Object value)
        // throws IllegalAccessException, InvocationTargetException {
        // if (value == null)
        // return;
        // if (value instanceof Integer && ((Integer) value).intValue() == 0)
        // return;
        // if (value instanceof Long && ((Long) value).longValue() == 0)
        // return;
        // if (value instanceof Double && ((Double) value).doubleValue() == 0)
        // return;
        // super.copyProperty(obj, name, value);
        // }
        // };
    }

}