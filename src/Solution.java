import java.util.ArrayList;
import java.util.HashMap;

public class Solution {
    Parameters parameters;
    ArrayList<ArrayList<Customer>> vehicleRoutes;
    ArrayList<RobotNode> robotNodes;
    ArrayList<Customer> customers;
    Algorithm algorithm;
    ArrayList<Integer> resiCapacity;
    ArrayList<Integer> resiRobotNum;
    double totalCost;
    //初始解构造：贪心
    public void initialSolution(DataRead dataRead){
        parameters = dataRead.parameters;
        algorithm = new Algorithm();
        resiCapacity = new ArrayList<>();
        resiRobotNum = new ArrayList<>();
        //按需求降序排列
        customers = new ArrayList<>(dataRead.customers);
        customers.remove(0);
        for (int j=0;j<customers.size()-1;j++) {
            for (int i=0;i<customers.size()-1-j;i++) {
                if (customers.get(i).demand < customers.get(i+1).demand){
                    Customer customer = customers.get(i);
                    customers.set(i, customers.get(i+1));
                    customers.set(i+1, customer);
                }
            }
        }
        //生成初始解，首先将需求最大的客户点插入，首尾均为仓库
        ArrayList<Customer> route1 = new ArrayList<>();
        route1.add(dataRead.customers.get(0));
        route1.add(customers.get(0));
        route1.add(dataRead.customers.get(0));
        vehicleRoutes = new ArrayList<>();
        vehicleRoutes.add(route1);
        robotNodes = new ArrayList<>();
        int resiC = parameters.vehicleCapacity - customers.get(0).demand;
        resiCapacity.add(resiC);
        resiRobotNum.add(parameters.robotNum);
        for(int i=1;i<customers.size();i++){
            //按照需求递减插入客户节点
            insertNode(this, customers.get(i));
        }
        //System.out.print("初始解建立");
        //System.out.println(Algorithm.solutionFeasible(this));
        //System.out.println();
    }
    //以总时间作为插入成本，返回节点直接插入路径的成本
    public double insertNode(Solution solution, Customer node){
        //选择包括：插入现有路径，建立新路径，用机器人服务
        double vehicleMinCost = Double.MAX_VALUE;
        int bestRoute = -1;
        double bestLocation = -1;
        boolean newRoute = false;
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            ArrayList<Double> res = vehicleFeasible(solution, i, node);
            if(res != null) {
                if(res.get(1) < vehicleMinCost){
                    vehicleMinCost = res.get(1);
                    bestRoute = i;
                    bestLocation = res.get(0);
                }
            }
        }
        //如果找不到现有路径可以插入，尝试建立新路径
        if(bestRoute == -1){
            vehicleMinCost = node.vehicleServiceTime +
                    (double) (2 * solution.parameters.distance[solution.vehicleRoutes.get(0).get(0).nodeNum][node.nodeNum]) / solution.parameters.vehicleSpeed;
            newRoute = true;
        }
        //尝试用机器人服务
        double robotMinCost = Double.MAX_VALUE;
        int bestRoute_r = -1;
        double bestLocation_r = -1;
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            ArrayList<Double> res = robotFeasible(solution, i, node);
            if(res != null) {
                if(res.get(1) < vehicleMinCost){
                    robotMinCost = res.get(1);
                    bestRoute_r = i;
                    bestLocation_r = res.get(0);
                }
            }
        }
        if(vehicleMinCost < robotMinCost){
            if(newRoute) {
                insertVehicle(solution, -1, -1, node, true);
                //System.out.print("插入车辆节点（新建）");
                //System.out.println(Algorithm.solutionFeasible(solution));
            }
            else {
                insertVehicle(solution, bestRoute, (int) bestLocation, node, false);
                //System.out.print("插入车辆节点");
                //System.out.println(Algorithm.solutionFeasible(solution));
            }

            return vehicleMinCost;
        }
        else {
            insertRobot(solution, bestRoute_r, (int) bestLocation_r, node);
            //System.out.print("插入机器人节点");
            //System.out.println(Algorithm.solutionFeasible(solution));
            return robotMinCost;
        }

    }
    public void insertVehicle(Solution solution, int routeID, int nodeID, Customer node, boolean newRoute){
        //以货车服务的形式插入节点
        if(newRoute) {
            ArrayList<Customer> route = new ArrayList<>();
            route.add(solution.vehicleRoutes.get(0).get(0));
            route.add(node);
            route.add(solution.vehicleRoutes.get(0).get(solution.vehicleRoutes.get(0).size()-1));
            solution.vehicleRoutes.add(route);
            solution.resiCapacity.add(solution.parameters.vehicleCapacity - node.demand);
            solution.resiRobotNum.add(solution.parameters.robotNum);
        }
        else{
            solution.vehicleRoutes.get(routeID).add(nodeID, node);
            solution.resiCapacity.set(routeID, solution.resiCapacity.get(routeID) - node.demand);
        }
        algorithm.getTimeWindow(solution);
        solution.totalCost = Algorithm.getSolutionCost(solution);
    }
    public void insertRobot(Solution solution, int routeID, int nodeID, Customer node){
        //以机器人服务的形式插入节点
        solution.robotNodes.add(new RobotNode(solution.vehicleRoutes.get(routeID).get(nodeID), routeID, nodeID, node));
        ArrayList<Double> res = algorithm.renewNodeTW(solution, routeID, nodeID, node);
        solution.resiRobotNum.set(routeID, solution.resiRobotNum.get(routeID) - 1);
        algorithm.getTimeWindow(solution);
        solution.resiCapacity.set(routeID, solution.resiCapacity.get(routeID) - node.demand);
        solution.totalCost = Algorithm.getSolutionCost(solution);
    }
    public ArrayList<Double> robotFeasible(Solution solution, int routeID, Customer node){
        //验证当前节点插入当前路径并用机器人服务的可行性
        if(node.robotAccess == 0) return null;
        if(solution.resiCapacity.get(routeID) < node.demand) return null;
        if(solution.resiRobotNum.get(routeID) < 1) return null;
        HashMap<Integer, Double> insertCost = new HashMap<>();
        for(int i=1;i<solution.vehicleRoutes.get(routeID).size()-1;i++){
            insertCost.put(i, Double.MAX_VALUE);
            if(solution.parameters.distance[solution.vehicleRoutes.get(routeID).get(i).nodeNum][node.nodeNum] > solution.parameters.robotRadius) continue;
            ArrayList<Double> timeWindow = algorithm.renewNodeTW(solution, routeID, i, node);
            double curTime = solution.vehicleRoutes.get(routeID).get(0).readyTime;
            boolean flag = true;
            for(int j=1;j<solution.vehicleRoutes.get(routeID).size();j++){
                if(j == i){
                    curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(routeID).get(j - 1).nodeNum]
                            [solution.vehicleRoutes.get(routeID).get(j).nodeNum] / solution.parameters.vehicleSpeed;
                    if(curTime > timeWindow.get(2)) {
                        flag = false;
                        break;
                    }
                    if(curTime < timeWindow.get(0)) curTime = timeWindow.get(0);
                    curTime += timeWindow.get(1);
                }
                else {
                    double serviceTime = solution.vehicleRoutes.get(routeID).get(j).dummyServiceTime == Double.MAX_VALUE ?
                            solution.vehicleRoutes.get(routeID).get(j).vehicleServiceTime : solution.vehicleRoutes.get(routeID).get(j).dummyServiceTime;
                    double readyTime = solution.vehicleRoutes.get(routeID).get(j).dummyReadyTime == Double.MAX_VALUE ?
                            solution.vehicleRoutes.get(routeID).get(j).readyTime : solution.vehicleRoutes.get(routeID).get(j).dummyReadyTime;
                    double dueTime = solution.vehicleRoutes.get(routeID).get(j).dummyDueTime == Double.MAX_VALUE ?
                            solution.vehicleRoutes.get(routeID).get(j).dueTime : solution.vehicleRoutes.get(routeID).get(j).dummyDueTime;
                    curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(routeID).get(j - 1).nodeNum]
                            [solution.vehicleRoutes.get(routeID).get(j).nodeNum] / solution.parameters.vehicleSpeed;
                    if (curTime > dueTime) {
                        flag = false;
                        break;
                    }
                    if (curTime < readyTime) curTime = readyTime;
                    curTime += serviceTime;
                }

            }
            if(flag) {
                insertCost.put(i, curTime - Algorithm.getRouteCost(solution, routeID));
                //if(curTime - Algorithm.getRouteCost(solution, routeID) < 0)
                    //System.out.println();
            }

        }
        double minCost = Double.MAX_VALUE;
        int bestLocation = -1;
        for(int i=1;i<solution.vehicleRoutes.get(routeID).size()-1;i++){
            if(insertCost.get(i) < minCost) {
                minCost = insertCost.get(i);
                bestLocation = i;
            }
        }
        if(minCost == Double.MAX_VALUE) return null;
        ArrayList<Double> res = new ArrayList<>();
        res.add((double) bestLocation);
        res.add(insertCost.get(bestLocation));
        return res;
    }
    public ArrayList<Double> vehicleFeasible(Solution solution, int routeID, Customer node){
        //验证货车服务可行性
        if(solution.resiCapacity.get(routeID) < node.demand) return null;

        HashMap<Integer, Double> insertCost = new HashMap<>();
        for(int i=1;i<solution.vehicleRoutes.get(routeID).size()-1;i++){
            insertCost.put(i, Double.MAX_VALUE);
            double curTime = solution.vehicleRoutes.get(routeID).get(0).readyTime;
            boolean flag = true;
            //首先判断时间窗合理性
            for(int j=1;j<i;j++){
                double serviceTime = solution.vehicleRoutes.get(routeID).get(j).dummyServiceTime == Double.MAX_VALUE?
                        solution.vehicleRoutes.get(routeID).get(j).vehicleServiceTime:solution.vehicleRoutes.get(routeID).get(j).dummyServiceTime;
                double readyTime = solution.vehicleRoutes.get(routeID).get(j).dummyReadyTime == Double.MAX_VALUE?
                        solution.vehicleRoutes.get(routeID).get(j).readyTime:solution.vehicleRoutes.get(routeID).get(j).dummyReadyTime;
                curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(routeID).get(j - 1).nodeNum]
                        [solution.vehicleRoutes.get(routeID).get(j).nodeNum] / solution.parameters.vehicleSpeed;
                if(curTime < readyTime) curTime = readyTime;
                curTime += serviceTime;

            }
            curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(routeID).get(i - 1).nodeNum][node.nodeNum] / solution.parameters.vehicleSpeed;
            double nodeReadyTime = node.dummyReadyTime == Double.MAX_VALUE?node.readyTime:node.dummyReadyTime;
            double nodeServiceTime = node.dummyServiceTime == Double.MAX_VALUE?node.vehicleServiceTime:node.dummyServiceTime;
            double nodeDueTime = node.dummyDueTime == Double.MAX_VALUE?node.dueTime:node.dummyDueTime;
            if(curTime > nodeDueTime) continue;
            if(curTime < nodeReadyTime) curTime = nodeReadyTime;
            //尝试插入当前位置并得到插入成本
            curTime += nodeServiceTime;
            curTime += (double) solution.parameters.distance[node.nodeNum][solution.vehicleRoutes.get(routeID).get(i).nodeNum] / solution.parameters.vehicleSpeed;
            for(int j=i;j<solution.vehicleRoutes.get(routeID).size();j++){
                double readyTime = solution.vehicleRoutes.get(routeID).get(j).dummyReadyTime == Double.MAX_VALUE?
                        solution.vehicleRoutes.get(routeID).get(j).readyTime:solution.vehicleRoutes.get(routeID).get(j).dummyReadyTime;
                double serviceTime = solution.vehicleRoutes.get(routeID).get(j).dummyServiceTime == Double.MAX_VALUE?
                        solution.vehicleRoutes.get(routeID).get(j).vehicleServiceTime:solution.vehicleRoutes.get(routeID).get(j).dummyServiceTime;
                double dueTime = solution.vehicleRoutes.get(routeID).get(j).dummyDueTime == Double.MAX_VALUE?
                        solution.vehicleRoutes.get(routeID).get(j).dueTime:solution.vehicleRoutes.get(routeID).get(j).dummyDueTime;
                if(j > i) curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(routeID).get(j - 1).nodeNum]
                        [solution.vehicleRoutes.get(routeID).get(j).nodeNum] / solution.parameters.vehicleSpeed;
                if(curTime > dueTime) {
                    flag = false;
                    break;
                }
                if(curTime < readyTime) curTime = readyTime;
                curTime += serviceTime;

            }
            if(flag) insertCost.put(i, curTime - Algorithm.getRouteCost(solution, routeID));
        }
        double minCost = Double.MAX_VALUE;
        int bestLocation = -1;
        for(int i=1;i<solution.vehicleRoutes.get(routeID).size()-1;i++){
            if(insertCost.get(i) < minCost) {
                minCost = insertCost.get(i);
                bestLocation = i;
            }
        }
        if(minCost == Double.MAX_VALUE) return null;
        ArrayList<Double> res = new ArrayList<>();
        res.add((double) bestLocation);
        res.add(insertCost.get(bestLocation));
        return res;
    }
    public Solution solutionClone(Solution solution){
        //解的复制
        Solution solution1 = new Solution();
        solution1.parameters = solution.parameters;
        ArrayList<ArrayList<Customer>> newVehicleRoutes = new ArrayList<>();
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            ArrayList<Customer> route = new ArrayList<>();
            for(int j=0;j<solution.vehicleRoutes.get(i).size();j++){
                route.add(Customer.customerClone(solution.vehicleRoutes.get(i).get(j)));
            }
            newVehicleRoutes.add(route);
        }
        solution1.vehicleRoutes = newVehicleRoutes;
        ArrayList<RobotNode> newRobotNodes = new ArrayList<>();
        for (RobotNode robotNode : robotNodes) {
            newRobotNodes.add(RobotNode.robotNodeClone(robotNode));
        }
        solution1.robotNodes = newRobotNodes;
        ArrayList<Customer> newCustomers = new ArrayList<>();
        for(int i=0;i<solution.customers.size();i++){
            newCustomers.add(Customer.customerClone(solution.customers.get(i)));
        }
        solution1.customers = newCustomers;
        solution1.algorithm = new Algorithm();
        solution1.resiCapacity = new ArrayList<>();
        solution1.resiCapacity.addAll(solution.resiCapacity);
        solution1.resiRobotNum = new ArrayList<>();
        solution1.resiRobotNum.addAll(solution.resiRobotNum);
        solution1.totalCost = solution.totalCost;
        return solution1;
    }
}
class RobotNode{
    //由机器人访问的节点
    Customer rootNode;  //出发节点
    int route_num;  //根节点所在路径序号
    int node_num;  //根节点序号
    Customer curNode;  //当前顾客点
    public RobotNode(Customer rootNode, int route_num, int node_num, Customer curNode){
        this.rootNode = rootNode;
        this.route_num = route_num;
        this.node_num = node_num;
        this.curNode = curNode;
    }
    public static RobotNode robotNodeClone(RobotNode robotNode){
       return new RobotNode(Customer.customerClone(robotNode.rootNode), robotNode.route_num, robotNode.node_num, Customer.customerClone(robotNode.curNode));
    }
}
