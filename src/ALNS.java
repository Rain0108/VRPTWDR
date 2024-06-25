import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ALNS {
    DataRead dataRead;
    public Solution ALNS_run(Solution solution){
        Solution curSolution = solution.solutionClone(solution);
        Solution bestSolution = solution.solutionClone(solution);
        int iter = 0;
        double[] initialDestroyWeight = {0.25, 0.25, 0.25, 0.25};
        double[] initialRebuildWeight = {1, 1, 1};
        for(int i=0;i<initialRebuildWeight.length;i++){
            initialRebuildWeight[i] /= 3;
        }
        double[] destroyScore = new double[4];
        double[] rebuildScore = new double[3];
        int[] destroyUseTimes = new int[4];
        int[] rebuildUseTimes = new int[3];
        double temper = solution.parameters.T;
        int removeNum = solution.parameters.removeMaxNum;
        while(iter < solution.parameters.maxIter){
            //System.out.println(iter);
            while(temper > solution.parameters.minT) {
                //轮盘赌法选择删除和插入算子并更新使用次数
                int destroyChoice = selectOperator(initialDestroyWeight);
                int rebuildChoice = selectOperator(initialRebuildWeight);
                destroyUseTimes[destroyChoice]++;
                rebuildUseTimes[rebuildChoice]++;
                ArrayList<Customer> removeCustomers;
                //System.out.println("curSolution");
                //System.out.println(Algorithm.solutionFeasible(curSolution));
                Solution tempSolution = curSolution.solutionClone(curSolution);
                //System.out.println(Algorithm.solutionFeasible(tempSolution));
                //根据选择的算子执行删除操作
                if (destroyChoice == 0) {
                    removeCustomers = distanceRemove(tempSolution, removeNum);
                } else if (destroyChoice == 1) {
                    removeCustomers = timeWindowRemove(tempSolution, removeNum);
                } else if(destroyChoice == 2){
                    removeCustomers = randomRemove(tempSolution, removeNum);
                }
                else{
                    removeCustomers = worstRemove(tempSolution, removeNum);
                }
                //更新时间窗
                tempSolution.algorithm.getTimeWindow(tempSolution);
                ArrayList<String> allCustomers = new ArrayList<>();
                for(Customer c : removeCustomers){
                    allCustomers.add(c.postCode);
                }
                for(int i=0;i<tempSolution.vehicleRoutes.size();i++){
                    for(int j=1;j<tempSolution.vehicleRoutes.get(i).size()-1;j++){
                        allCustomers.add(tempSolution.vehicleRoutes.get(i).get(j).postCode);
                    }
                }
                for(RobotNode c : tempSolution.robotNodes){
                    allCustomers.add(c.curNode.postCode);
                }
                for(int i=1;i<dataRead.customers.size();i++){
                    boolean flag = false;
                    for(int j=0;j<allCustomers.size();j++){
                        if(allCustomers.get(j).equals(dataRead.customers.get(i).postCode)) {
                            flag = true;
                            break;
                        }
                    }
                    //if(!flag)
                        //System.out.println();
                }
                //System.out.println("删除节点后");
                //System.out.println(Algorithm.solutionFeasible(tempSolution));
                //执行插入操作
                if (rebuildChoice == 0) {
                    tempSolution = randomInsert(tempSolution, removeCustomers);
                } else if (rebuildChoice == 1) {
                    tempSolution = timeWindowInsert(tempSolution, removeCustomers);
                } else {
                    tempSolution = greedyInsert(tempSolution, removeCustomers);
                    //System.out.println();
                }
                //System.out.println(Algorithm.solutionFeasible(tempSolution));
                /*
                allCustomers = new ArrayList<>();
                for(int i=0;i<tempSolution.vehicleRoutes.size();i++){
                    for(int j=1;j<tempSolution.vehicleRoutes.get(i).size()-1;j++){
                        allCustomers.add(tempSolution.vehicleRoutes.get(i).get(j).postCode);
                    }
                }
                for(int i=1;i<dataRead.customers.size();i++){
                    boolean flag = false;
                    for(int j=0;j<allCustomers.size();j++){
                        if(allCustomers.get(j).equals(dataRead.customers.get(i).postCode)) {
                            flag = true;
                            break;
                        }
                    }
                    if(!flag)
                        System.out.println();
                }
                */
                //根据结果更新最优解，当前解和算子评分
                tempSolution.algorithm.getTimeWindow(tempSolution);
                tempSolution.totalCost = Algorithm.getSolutionCost(tempSolution);
                if (tempSolution.totalCost < Algorithm.getSolutionCost(curSolution)) {
                    curSolution = tempSolution.solutionClone(tempSolution);
                    destroyScore[destroyChoice] += 1.2;
                    rebuildScore[rebuildChoice] += 1.2;
                } else {
                    if (Metrospolis(Algorithm.getSolutionCost(curSolution), tempSolution.totalCost, temper)) {
                        curSolution = tempSolution.solutionClone(tempSolution);
                        destroyScore[destroyChoice] += 0.8;
                        rebuildScore[rebuildChoice] += 0.8;
                    } else {
                        destroyScore[destroyChoice] += 0.6;
                        rebuildScore[rebuildChoice] += 0.6;
                    }
                }
                if (!Algorithm.solutionFeasible(tempSolution)) {
                    System.out.println("wrong");
                    break;
                }
                if (tempSolution.totalCost < Algorithm.getSolutionCost(bestSolution)) {
                    bestSolution = tempSolution.solutionClone(tempSolution);
                    destroyScore[destroyChoice] += 1.5;
                    rebuildScore[rebuildChoice] += 1.5;
                }
                for (int i = 0; i < destroyScore.length; i++) {
                    initialDestroyWeight[i] = destroyUseTimes[i] == 0?initialDestroyWeight[i]:(1 - solution.parameters.weightRenewSpeed) * initialDestroyWeight[i] + solution.parameters.weightRenewSpeed * destroyScore[i] / destroyUseTimes[i];
                    }
                for(int i=0;i< rebuildScore.length;i++){
                    initialRebuildWeight[i] = rebuildUseTimes[i] == 0?initialRebuildWeight[i]:(1 - solution.parameters.weightRenewSpeed) * initialRebuildWeight[i] + solution.parameters.weightRenewSpeed * rebuildScore[i] / rebuildUseTimes[i];
                }
                temper = temper * (1 - solution.parameters.temperDecSpeed);
            }
            iter ++;
            temper = solution.parameters.T;
            removeNum = (int) Math.max(solution.parameters.removeMinNum, removeNum * solution.parameters.removeNumDecSpeed);
        }
        ArrayList<Integer> validRouteID = new ArrayList<>();
        for(int i=0;i<bestSolution.vehicleRoutes.size();i++){
            if(bestSolution.vehicleRoutes.get(i).size() > 2) {
                validRouteID.add(i);
            }
        }
        ArrayList<ArrayList<Customer>> vehicleRoutes = new ArrayList<>();
        ArrayList<Integer> resiC = new ArrayList<>();
        ArrayList<Integer> resiR = new ArrayList<>();
        for(int i=0;i<validRouteID.size();i++){
            vehicleRoutes.add(bestSolution.vehicleRoutes.get(i));
            resiC.add(bestSolution.resiCapacity.get(i));
            resiR.add(bestSolution.resiRobotNum.get(i));
        }
        bestSolution.vehicleRoutes = vehicleRoutes;
        bestSolution.resiCapacity = resiC;
        bestSolution.resiRobotNum = resiR;
        bestSolution.totalCost = Algorithm.getSolutionCost(bestSolution);
        return bestSolution;
    }
    public int selectOperator(double[] weight){
        //轮盘赌法选择算子
        double sum = 0;
        for(int i=0;i<weight.length;i++){
            sum += weight[i];
        }
        for(int i=0;i<weight.length;i++){
            weight[i] = weight[i] / sum;
        }
        double[] sumWeight = new double[weight.length];
        for(int i=0;i<weight.length;i++){
            for(int j=0;j<=i;j++){
                sumWeight[i] += weight[j];
            }
        }
        double randomNum = Math.random();
        for(int i=0;i<sumWeight.length-1;i++){
            if(randomNum <= sumWeight[i]){
                return i;
            }
            else if(randomNum > sumWeight[i] && randomNum <= sumWeight[i+1]){
                return i+1;
            }
        }
        //System.out.println();
        return -1;
    }
    public ArrayList<Customer> distanceRemove(Solution solution, int removeNum){
        ArrayList<Customer> customers = new ArrayList<>();
        for(int j=1;j<dataRead.customers.size();j++) {
            boolean flag = true;
            for (int i = 0; i < solution.robotNodes.size(); i++) {
                if (Objects.equals(dataRead.customers.get(j).postCode, solution.robotNodes.get(i).curNode.postCode)){
                    flag = false;
                    break;
                }
            }
            if(flag) customers.add(dataRead.customers.get(j));
        }
        ArrayList<Customer> removeNodes = new ArrayList<>();
        int removeID = solution.parameters.random.nextInt(customers.size());
        int curRemoveNum = 0;
        while (curRemoveNum < removeNum){
            removeNodes.add(customers.get(removeID));
            for(int i=0;i<solution.robotNodes.size();i++){
                if(Objects.equals(solution.robotNodes.get(i).rootNode.postCode, customers.get(removeID).postCode)){
                    removeNodes.add(solution.robotNodes.get(i).curNode);
                }
            }
            remove(solution, customers.get(removeID));
            curRemoveNum ++;
            int minDis = Integer.MAX_VALUE;
            int minID = -1;
            for(int i=0;i<customers.size();i++){
                boolean flag = true;
                for (Customer removeNode : removeNodes) {
                    if (Objects.equals(removeNode.postCode, customers.get(i).postCode)) {
                        flag = false;
                        break;
                    }
                }
                if(flag){
                    try {
                        if (solution.parameters.distance[Algorithm.getID(dataRead, customers.get(i))][Algorithm.getID(dataRead, customers.get(removeID))] < minDis) {
                            minDis = solution.parameters.distance[Algorithm.getID(dataRead, customers.get(i))][Algorithm.getID(dataRead, customers.get(removeID))];
                            minID = i;
                        }
                    }catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException){
                        System.out.println();
                    }
                }
            }
            removeID = minID;
        }
        ArrayList<Integer> validRouteID = new ArrayList<>();
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.vehicleRoutes.get(i).size() > 2) {
                validRouteID.add(i);
            }
        }
        ArrayList<ArrayList<Customer>> vehicleRoutes = new ArrayList<>();
        ArrayList<Integer> resiC = new ArrayList<>();
        ArrayList<Integer> resiR = new ArrayList<>();
        for(Integer i : validRouteID){
            vehicleRoutes.add(solution.vehicleRoutes.get(i));
            resiC.add(solution.resiCapacity.get(i));
            resiR.add(solution.resiRobotNum.get(i));
        }
        solution.vehicleRoutes = vehicleRoutes;
        solution.resiCapacity = resiC;
        solution.resiRobotNum = resiR;
        solution.totalCost = Algorithm.getSolutionCost(solution);
        return removeNodes;
    }
    public ArrayList<Customer> timeWindowRemove(Solution solution, int removeNum){
        ArrayList<Customer> customers = new ArrayList<>();
        for(int j=1;j<dataRead.customers.size();j++) {
            boolean flag = true;
            for (int i = 0; i < solution.robotNodes.size(); i++) {
                if (Objects.equals(dataRead.customers.get(j).postCode, solution.robotNodes.get(i).curNode.postCode)){
                    flag = false;
                    break;
                }
            }
            if(flag) customers.add(dataRead.customers.get(j));
        }
        ArrayList<Customer> removeNodes = new ArrayList<>();
        int removeID = solution.parameters.random.nextInt(customers.size());
        int curRemoveNum = 0;
        while (curRemoveNum < removeNum){
            removeNodes.add(customers.get(removeID));
            for(int i=0;i<solution.robotNodes.size();i++){
                if(Objects.equals(solution.robotNodes.get(i).rootNode.postCode, customers.get(removeID).postCode)){
                    removeNodes.add(solution.robotNodes.get(i).curNode);
                }
            }
            remove(solution, customers.get(removeID));
            curRemoveNum ++;
            double minRC = Integer.MAX_VALUE;
            int minID = -1;
            for(int i=0;i<customers.size();i++){
                boolean flag = true;
                for (Customer removeNode : removeNodes) {
                    if (Objects.equals(removeNode.postCode, customers.get(i).postCode)) {
                        flag = false;
                        break;
                    }
                }
                if(flag){
                    double l1 = customers.get(i).dummyReadyTime == Double.MAX_VALUE?customers.get(i).readyTime:customers.get(i).dummyReadyTime;
                    double l2 = customers.get(removeID).dummyReadyTime == Double.MAX_VALUE?customers.get(removeID).readyTime:customers.get(removeID).dummyReadyTime;
                    double u1 = customers.get(i).dummyDueTime == Double.MAX_VALUE?customers.get(i).dueTime:customers.get(i).dummyDueTime;
                    double u2 = customers.get(removeID).dummyDueTime == Double.MAX_VALUE?customers.get(removeID).dueTime:customers.get(removeID).dummyDueTime;
                    double res = Math.abs(l1-l2)+Math.abs(u1-u2);
                    if(res < minRC){
                        minRC = res;
                        minID = i;
                    }
                }
            }
            removeID = minID;
        }

        ArrayList<Integer> validRouteID = new ArrayList<>();
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.vehicleRoutes.get(i).size() > 2) {
                validRouteID.add(i);
            }
        }
        ArrayList<ArrayList<Customer>> vehicleRoutes = new ArrayList<>();
        ArrayList<Integer> resiC = new ArrayList<>();
        ArrayList<Integer> resiR = new ArrayList<>();
        for(Integer i : validRouteID){
            vehicleRoutes.add(solution.vehicleRoutes.get(i));
            resiC.add(solution.resiCapacity.get(i));
            resiR.add(solution.resiRobotNum.get(i));
        }
        solution.vehicleRoutes = vehicleRoutes;
        solution.resiCapacity = resiC;
        solution.resiRobotNum = resiR;
        solution.totalCost = Algorithm.getSolutionCost(solution);


        return removeNodes;
    }
    public ArrayList<Customer> randomRemove(Solution solution, int removeNum){
        ArrayList<Customer> customers = new ArrayList<>();
        for(int j=1;j<dataRead.customers.size();j++) {
            boolean flag = true;
            for (int i = 0; i < solution.robotNodes.size(); i++) {
                if (Objects.equals(dataRead.customers.get(j).postCode, solution.robotNodes.get(i).curNode.postCode)){
                    flag = false;
                    break;
                }
            }
            if(flag) customers.add(dataRead.customers.get(j));
        }
        ArrayList<Customer> removeNodes = new ArrayList<>();
        int removeID = solution.parameters.random.nextInt(customers.size());
        int curRemoveNum = 0;
        while (curRemoveNum < removeNum){
            try {

                removeNodes.add(customers.get(removeID));
            }catch (IndexOutOfBoundsException e){
                System.out.println();
            }
            for(int i=0;i<solution.robotNodes.size();i++){
                if(Objects.equals(solution.robotNodes.get(i).rootNode.postCode, customers.get(removeID).postCode)){
                    removeNodes.add(solution.robotNodes.get(i).curNode);
                }
            }
            remove(solution, customers.get(removeID));

            curRemoveNum ++;
            ArrayList<Integer> nodes = new ArrayList<>();
            for (Customer customer : customers) {
                boolean flag = true;
                for (Customer removeNode : removeNodes) {
                    if (Objects.equals(removeNode.postCode, customer.postCode)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    nodes.add(customer.nodeNum);
                }
            }
            int removeNodeNum = nodes.get(solution.parameters.random.nextInt(nodes.size()));
            for(int i=0;i<customers.size();i++){
                if(customers.get(i).nodeNum == removeNodeNum) {
                    removeID = i;
                    break;
                }
            }
            if(removeID > customers.size())
                System.out.println();
        }

        ArrayList<Integer> validRouteID = new ArrayList<>();
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.vehicleRoutes.get(i).size() > 2) {
                validRouteID.add(i);
            }
        }
        ArrayList<ArrayList<Customer>> vehicleRoutes = new ArrayList<>();
        ArrayList<Integer> resiC = new ArrayList<>();
        ArrayList<Integer> resiR = new ArrayList<>();
        for(Integer i : validRouteID){
            vehicleRoutes.add(solution.vehicleRoutes.get(i));
            resiC.add(solution.resiCapacity.get(i));
            resiR.add(solution.resiRobotNum.get(i));
        }
        solution.vehicleRoutes = vehicleRoutes;
        solution.resiCapacity = resiC;
        solution.resiRobotNum = resiR;
        solution.totalCost = Algorithm.getSolutionCost(solution);

        return removeNodes;
    }
    public ArrayList<Customer> worstRemove(Solution solution, int removeNum){
        ArrayList<Customer> customers = new ArrayList<>();
        for(int j=1;j<dataRead.customers.size();j++) {
            boolean flag = true;
            for (int i = 0; i < solution.robotNodes.size(); i++) {
                if (Objects.equals(dataRead.customers.get(j).postCode, solution.robotNodes.get(i).curNode.postCode)){
                    flag = false;
                    break;
                }
            }
            if(flag) customers.add(dataRead.customers.get(j));
        }
        ArrayList<Double> removeCost = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();
        for(int i=0;i<customers.size();i++){
            double cost1 = solution.totalCost;
            Solution solution1 = solution.solutionClone(solution);
            remove(solution1, customers.get(i));
            if(cost1 - solution1.totalCost > 0) {
                removeCost.add(cost1 - solution1.totalCost);
                order.add(i);
            }
        }
        for (int j=0;j<removeCost.size()-1;j++) {
            for (int i=0;i<removeCost.size()-1-j;i++) {
                if (removeCost.get(i) < removeCost.get(i+1)){
                    double d = removeCost.get(i);
                    removeCost.set(i, removeCost.get(i+1));
                    removeCost.set(i+1, d);
                    int temp = order.get(i);
                    order.set(i, order.get(i+1));
                    order.set(i+1, temp);
                }
            }
        }
        ArrayList<Customer> removeNodes = new ArrayList<>();
        int curRemoveNum = 0;

        while (curRemoveNum < removeNum){
            int removeID = order.get(curRemoveNum);
            removeNodes.add(customers.get(removeID));
            //把关联的机器人节点也一并删除
            for(int i=0;i<solution.robotNodes.size();i++){
                if(Objects.equals(solution.robotNodes.get(i).rootNode.postCode, customers.get(removeID).postCode)){
                    removeNodes.add(solution.robotNodes.get(i).curNode);
                }
            }
            remove(solution, customers.get(removeID));
            curRemoveNum ++;
        }
        ArrayList<Integer> validRouteID = new ArrayList<>();
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.vehicleRoutes.get(i).size() > 2) {
                validRouteID.add(i);
            }
        }
        ArrayList<ArrayList<Customer>> vehicleRoutes = new ArrayList<>();
        ArrayList<Integer> resiC = new ArrayList<>();
        ArrayList<Integer> resiR = new ArrayList<>();
        for(Integer i : validRouteID){
            vehicleRoutes.add(solution.vehicleRoutes.get(i));
            resiC.add(solution.resiCapacity.get(i));
            resiR.add(solution.resiRobotNum.get(i));
        }
        solution.vehicleRoutes = vehicleRoutes;
        solution.resiCapacity = resiC;
        solution.resiRobotNum = resiR;
        solution.totalCost = Algorithm.getSolutionCost(solution);
        return removeNodes;
    }
    public ArrayList<Customer> worstRobotRemove(Solution solution, int removeNum){
        ArrayList<Customer> customers = new ArrayList<>();
        for(int j=1;j<dataRead.customers.size();j++) {
            boolean flag = true;
            for (int i = 0; i < solution.robotNodes.size(); i++) {
                if (Objects.equals(dataRead.customers.get(j).postCode, solution.robotNodes.get(i).curNode.postCode)){
                    flag = false;
                    break;
                }
            }
            if(flag) customers.add(dataRead.customers.get(j));
        }
        ArrayList<Double> removeCost = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();
        for(int i=0;i<customers.size();i++){
            double cost1 = solution.totalCost;
            Solution solution1 = solution.solutionClone(solution);
            remove(solution1, customers.get(i));
            if(cost1 - solution1.totalCost > 0) {
                removeCost.add(cost1 - solution1.totalCost);
                order.add(i);
            }
        }
        for (int j=0;j<removeCost.size()-1;j++) {
            for (int i=0;i<removeCost.size()-1-j;i++) {
                if (removeCost.get(i) < removeCost.get(i+1)){
                    double d = removeCost.get(i);
                    removeCost.set(i, removeCost.get(i+1));
                    removeCost.set(i+1, d);
                    int temp = order.get(i);
                    order.set(i, order.get(i+1));
                    order.set(i+1, temp);
                }
            }
        }
        ArrayList<Customer> removeNodes = new ArrayList<>();
        int curRemoveNum = 0;

        while (curRemoveNum < removeNum){
            int removeID = order.get(curRemoveNum);
            removeNodes.add(customers.get(removeID));
            //把关联的机器人节点也一并删除
            for(int i=0;i<solution.robotNodes.size();i++){
                if(Objects.equals(solution.robotNodes.get(i).rootNode.postCode, customers.get(removeID).postCode)){
                    removeNodes.add(solution.robotNodes.get(i).curNode);
                }
            }
            remove(solution, customers.get(removeID));
            curRemoveNum ++;
        }
        return removeNodes;
    }
    public ArrayList<Customer> routeRemove(Solution solution, int removeNum){
        int removeRoute = -1;
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.vehicleRoutes.get(i).size()-2 <= removeNum){
                removeRoute = i;
                break;
            }
        }
        ArrayList<Customer> removeNodes = new ArrayList<>();
        if(removeRoute == -1) removeRoute = solution.parameters.random.nextInt(solution.vehicleRoutes.size());
        for(int j=0;j<solution.vehicleRoutes.get(removeRoute).size();j++) {
            if(j < solution.vehicleRoutes.get(removeRoute).size() - 1 && j > 0){
                removeNodes.add(solution.vehicleRoutes.get(removeRoute).get(j));
            }
            for (int i = 0; i < solution.robotNodes.size(); i++) {
                if (Objects.equals(solution.robotNodes.get(i).rootNode.postCode, solution.vehicleRoutes.get(removeRoute).get(j).postCode)) {
                    removeNodes.add(solution.robotNodes.get(i).curNode);
                }
            }
        }
        for (Customer removeNode : removeNodes) {
            remove(solution, removeNode);
        }
        return removeNodes;
    }
    public void remove(Solution solution, Customer customer){
        //从当前解中删除指定客户及其机器人关联客户
        int routeID = -1;
        int nodeID = -1;
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            for(int j=0;j<solution.vehicleRoutes.get(i).size();j++){
                if(Objects.equals(solution.vehicleRoutes.get(i).get(j).postCode, customer.postCode)){
                    routeID = i;
                    nodeID = j;
                    break;
                }
            }
        }
        if(routeID == -1)
            System.out.println();
        solution.vehicleRoutes.get(routeID).remove(nodeID);
        ArrayList<RobotNode> newRobotNodes = new ArrayList<>();
        int robotDemand = 0;
        int robotNum = 0;
        for(int i=0;i<solution.robotNodes.size();i++){
            if(Objects.equals(solution.robotNodes.get(i).rootNode.postCode, customer.postCode)) {
                robotDemand += solution.robotNodes.get(i).curNode.demand;
                robotNum ++;
                continue;
            }
            newRobotNodes.add(solution.robotNodes.get(i));
        }
        solution.robotNodes = newRobotNodes;
        solution.resiCapacity.set(routeID, solution.resiCapacity.get(routeID) + customer.demand + robotDemand);
        solution.resiRobotNum.set(routeID, solution.resiRobotNum.get(routeID) + robotNum);
        solution.totalCost = Algorithm.getSolutionCost(solution);
    }
    //插入算子：1.随机选择插入最佳位置 2.选择时间窗最窄的 3.选择机器人不能访问的 4.优先分配机器人，再分配车，选成本最小的
    public Solution randomInsert(Solution iniSolution, ArrayList<Customer> removeCustomers){
        //随机选择节点
        Solution solution = iniSolution.solutionClone(iniSolution);
        ArrayList<Customer> customers = new ArrayList<>(removeCustomers);
        while (customers.size() > 0){
            int nodeID = solution.parameters.random.nextInt(customers.size());
            //System.out.println("随机插入");
            //System.out.println(Algorithm.solutionFeasible(solution));
            solution.insertNode(solution, customers.get(nodeID));
            customers.remove(nodeID);
        }
        solution.totalCost = Algorithm.getSolutionCost(solution);
        return solution;
    }
    public Solution timeWindowInsert(Solution iniSolution, ArrayList<Customer> customers){
        Solution solution = iniSolution.solutionClone(iniSolution);
        ArrayList<Customer> removeCustomers = new ArrayList<>(customers);
        ArrayList<Double> timeWindows = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();
        for(int i=0;i<removeCustomers.size();i++){
            double l = removeCustomers.get(i).dummyReadyTime == Double.MAX_VALUE ? removeCustomers.get(i).readyTime : removeCustomers.get(i).dummyReadyTime;
            double u = removeCustomers.get(i).dummyDueTime == Double.MAX_VALUE ? removeCustomers.get(i).dueTime : removeCustomers.get(i).dummyDueTime;
            timeWindows.add(u - l);
            order.add(i);
        }
        for (int j=0;j<removeCustomers.size()-1;j++) {
            for (int i=0;i<removeCustomers.size()-1-j;i++) {
                if (timeWindows.get(i) < timeWindows.get(i+1)){
                    double t = timeWindows.get(i);
                    timeWindows.set(i, timeWindows.get(i+1));
                    timeWindows.set(i+1, t);
                    int temp = order.get(i);
                    order.set(i, order.get(i+1));
                    order.set(i+1, temp);
                }
            }
        }
        for(Integer i : order) {
            solution.insertNode(solution, removeCustomers.get(i));
        }
        //System.out.println(Algorithm.solutionFeasible(solution));
        solution.totalCost = Algorithm.getSolutionCost(solution);
        return solution;
    }
    public Solution greedyInsert(Solution iniSolution, ArrayList<Customer> removeCustomers){
        Solution solution = iniSolution.solutionClone(iniSolution);
        double minCost = Double.MAX_VALUE;
        int nodeID = -1;
        ArrayList<Double> costs = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();
        for(int i=0;i<removeCustomers.size();i++){
            Solution solution1 = solution.solutionClone(solution);
            double curCost = solution.insertNode(solution1, removeCustomers.get(i));
            costs.add(curCost);
            order.add(i);
        }
        for (int j=0;j<costs.size()-1;j++) {
            for (int i=0;i<costs.size()-1-j;i++) {
                if (costs.get(i) < costs.get(i+1)){
                    double t = costs.get(i);
                    costs.set(i, costs.get(i+1));
                    costs.set(i+1, t);
                    int temp = order.get(i);
                    order.set(i, order.get(i+1));
                    order.set(i+1, temp);
                }
            }
        }
        for(Integer i : order) solution.insertNode(solution, removeCustomers.get(i));
        solution.totalCost = Algorithm.getSolutionCost(solution);
        return solution;
    }
    public boolean Metrospolis(double f_old, double f_new, double T){
        double p = Math.exp((f_old - f_new) / T);
        return Math.random() < p;
    }
    public ALNS(DataRead dataRead){
        this.dataRead = dataRead;
    }
}
