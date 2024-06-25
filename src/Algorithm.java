import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class Algorithm {
    //功能函数
    public static boolean solutionFeasible(Solution solution){
        //检验解的合理性
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.resiCapacity.get(i) < 0) return false;
            if(solution.resiRobotNum.get(i) < 0) return false;
            double curTime = solution.vehicleRoutes.get(i).get(0).readyTime;
            for(int j=1;j<solution.vehicleRoutes.get(i).size();j++){
                double serviceTime = solution.vehicleRoutes.get(i).get(j).dummyServiceTime == Double.MAX_VALUE ?
                        solution.vehicleRoutes.get(i).get(j).vehicleServiceTime : solution.vehicleRoutes.get(i).get(j).dummyServiceTime;
                double readyTime = solution.vehicleRoutes.get(i).get(j).dummyReadyTime == Double.MAX_VALUE ?
                        solution.vehicleRoutes.get(i).get(j).readyTime : solution.vehicleRoutes.get(i).get(j).dummyReadyTime;
                double dueTime = solution.vehicleRoutes.get(i).get(j).dummyDueTime == Double.MAX_VALUE ?
                        solution.vehicleRoutes.get(i).get(j).dueTime : solution.vehicleRoutes.get(i).get(j).dummyDueTime;

                curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(i).get(j - 1).nodeNum]
                        [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.vehicleSpeed;
                if(curTime > dueTime)
                    return false;
                if(curTime < readyTime) curTime = readyTime;
                curTime += serviceTime;
            }
        }
        int curCusNum = 0;
        for(int i=0;i<solution.vehicleRoutes.size();i++){
            if(solution.vehicleRoutes.get(i).size() == 2) continue;
            curCusNum += (solution.vehicleRoutes.get(i).size()-2);
        }
        curCusNum += solution.robotNodes.size();
        if(curCusNum != solution.customers.size()) return false;
        return true;
    }
    public static int getID(DataRead dataRead, Customer customer){
        for(int i=0;i<dataRead.customers.size();i++){
            if(Objects.equals(dataRead.customers.get(i).postCode, customer.postCode)) return dataRead.customers.get(i).nodeNum;
        }
        return -1;
    }
    public static double getSolutionCost(Solution solution){
        //以总时间计算成本
        double totalTime = 0;
        for(int i=0;i<solution.vehicleRoutes.size();i++) {
            double curTime = solution.vehicleRoutes.get(i).get(0).readyTime;
            for(int j=1;j<solution.vehicleRoutes.get(i).size();j++){
                try {

                    curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(i).get(j - 1).nodeNum][solution.vehicleRoutes.
                            get(i).get(j).nodeNum] / solution.parameters.vehicleSpeed;
                }catch (NullPointerException e){
                    System.out.println();
                }
                double curReadyTime;
                double curServiceTime;
                if(solution.vehicleRoutes.get(i).get(j).dummyReadyTime != Double.MAX_VALUE) curReadyTime = solution.vehicleRoutes.get(i).get(j).dummyReadyTime;
                else curReadyTime = solution.vehicleRoutes.get(i).get(j).readyTime;
                if(solution.vehicleRoutes.get(i).get(j).dummyServiceTime != Double.MAX_VALUE) curServiceTime = solution.vehicleRoutes.get(i).get(j).dummyServiceTime;
                else curServiceTime = solution.vehicleRoutes.get(i).get(j).vehicleServiceTime;
                if(curTime < curReadyTime) curTime = curReadyTime;
                curTime += curServiceTime;
            }
            totalTime += (curTime-solution.vehicleRoutes.get(i).get(0).readyTime);
        }
        return totalTime;
    }

    public static double getRouteCost(Solution solution, int i){
        //以总时间计算成本
        double curTime = solution.vehicleRoutes.get(i).get(0).readyTime;
        for(int j=1;j<solution.vehicleRoutes.get(i).size();j++){
            curTime += (double) solution.parameters.distance[solution.vehicleRoutes.get(i).get(j - 1).nodeNum][solution.vehicleRoutes.
                    get(i).get(j).nodeNum] / solution.parameters.vehicleSpeed;
            double curReadyTime;
            double curServiceTime;
            double curDueTime;
            if(solution.vehicleRoutes.get(i).get(j).dummyReadyTime != Double.MAX_VALUE) curReadyTime = solution.vehicleRoutes.get(i).get(j).dummyReadyTime;
            else curReadyTime = solution.vehicleRoutes.get(i).get(j).readyTime;
            if(solution.vehicleRoutes.get(i).get(j).dummyServiceTime != Double.MAX_VALUE) curServiceTime = solution.vehicleRoutes.get(i).get(j).dummyServiceTime;
            else curServiceTime = solution.vehicleRoutes.get(i).get(j).vehicleServiceTime;
            if(solution.vehicleRoutes.get(i).get(j).dummyDueTime != Double.MAX_VALUE) curDueTime = solution.vehicleRoutes.get(i).get(j).dummyDueTime;
            else curDueTime = solution.vehicleRoutes.get(i).get(j).dueTime;
            if(curTime < curReadyTime) curTime = curReadyTime;
            if(curTime > curDueTime)
                System.out.println();
            curTime += curServiceTime;
        }
        return curTime;
    }
    public void getTimeWindow(Solution solution){
        //更新当前解的时间窗
        for(int k=0;k<solution.robotNodes.size();k++) {
            for (int i = 0; i < solution.vehicleRoutes.size(); i++) {
                for (int j = 0; j < solution.vehicleRoutes.get(i).size(); j++) {
                    if(solution.robotNodes.get(k).rootNode.nodeNum == solution.vehicleRoutes.get(i).get(j).nodeNum){
                        //更新当前根节点的时间窗
                        if(solution.vehicleRoutes.get(i).get(j).dummyDueTime != Double.MAX_VALUE) {
                            solution.vehicleRoutes.get(i).get(j).dummyDueTime = Math.min(Math.min(solution.vehicleRoutes.get(i).get(j).dueTime,
                                    solution.robotNodes.get(k).curNode.dueTime - solution.parameters.distance[solution.robotNodes.get(k).curNode.nodeNum]
                                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed), solution.vehicleRoutes.get(i).get(j).dummyDueTime);
                        }else{
                            solution.vehicleRoutes.get(i).get(j).dummyDueTime = Math.min(solution.vehicleRoutes.get(i).get(j).dueTime,
                                    solution.robotNodes.get(k).curNode.dueTime - solution.parameters.distance[solution.robotNodes.get(k).curNode.nodeNum]
                                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed);
                        }
                        if(solution.vehicleRoutes.get(i).get(j).dummyServiceTime != Double.MAX_VALUE) {
                            solution.vehicleRoutes.get(i).get(j).dummyServiceTime = Math.max(Math.max(solution.vehicleRoutes.get(i).get(j).vehicleServiceTime + solution.vehicleRoutes.get(i).get(j).readyTime,
                                    solution.robotNodes.get(k).curNode.readyTime + solution.robotNodes.get(k).curNode.robotServiceTime + solution.parameters.distance[solution.robotNodes.get(k).curNode.nodeNum]
                                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed) - getLi(solution.vehicleRoutes.get(i).get(j), solution.robotNodes.get(k).curNode, solution), solution.vehicleRoutes.get(i).get(j).dummyServiceTime);
                        } else{
                            solution.vehicleRoutes.get(i).get(j).dummyServiceTime = Math.max(solution.vehicleRoutes.get(i).get(j).vehicleServiceTime + solution.vehicleRoutes.get(i).get(j).readyTime,
                                    solution.robotNodes.get(k).curNode.readyTime + solution.robotNodes.get(k).curNode.robotServiceTime + solution.parameters.distance[solution.robotNodes.get(k).curNode.nodeNum]
                                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed) - getLi(solution.vehicleRoutes.get(i).get(j), solution.robotNodes.get(k).curNode, solution);
                        }
                        if(solution.vehicleRoutes.get(i).get(j).dummyReadyTime != Double.MAX_VALUE) {
                            solution.vehicleRoutes.get(i).get(j).dummyReadyTime = Math.max(solution.vehicleRoutes.get(i).get(j).dummyReadyTime, getLi(solution.vehicleRoutes.get(i).get(j), solution.robotNodes.get(k).curNode, solution));
                        }else{
                            solution.vehicleRoutes.get(i).get(j).dummyReadyTime = getLi(solution.vehicleRoutes.get(i).get(j), solution.robotNodes.get(k).curNode, solution);

                        }
                    }
                }
            }
        }
    }
    public ArrayList<Double> renewNodeTW(Solution solution, int i, int j, Customer robotNode){
        //更新当前节点的时间窗
        ArrayList<Double> res = new ArrayList<>();
        double dummyReadyTime;
        double dummyServiceTime;
        double dummyDueTime;
        /*
        double dummyDueTime = Math.min(solution.vehicleRoutes.get(i).get(j).dueTime,
                robotNode.dueTime - solution.parameters.distance[robotNode.nodeNum][solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed);
        double dummyReadyTime = getLi(solution.vehicleRoutes.get(i).get(j), robotNode, solution);
        double dummyServiceTime = Math.max(solution.vehicleRoutes.get(i).get(j).vehicleServiceTime + solution.vehicleRoutes.get(i).get(j).readyTime,
                robotNode.readyTime + robotNode.robotServiceTime + solution.parameters.distance[robotNode.nodeNum][solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed) - dummyReadyTime;
         */
        if(solution.vehicleRoutes.get(i).get(j).dummyDueTime != Double.MAX_VALUE) {
            dummyDueTime = Math.min(Math.min(solution.vehicleRoutes.get(i).get(j).dueTime,
                    robotNode.dueTime - solution.parameters.distance[robotNode.nodeNum]
                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed), solution.vehicleRoutes.get(i).get(j).dummyDueTime);
        }else{
            dummyDueTime = Math.min(solution.vehicleRoutes.get(i).get(j).dueTime,
                    robotNode.dueTime - solution.parameters.distance[robotNode.nodeNum]
                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed);
        }
        if(solution.vehicleRoutes.get(i).get(j).dummyServiceTime != Double.MAX_VALUE) {
            dummyServiceTime = Math.max(Math.max(solution.vehicleRoutes.get(i).get(j).vehicleServiceTime + solution.vehicleRoutes.get(i).get(j).readyTime,
                    robotNode.readyTime + robotNode.robotServiceTime + solution.parameters.distance[robotNode.nodeNum]
                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed) - getLi(solution.vehicleRoutes.get(i).get(j), robotNode, solution), solution.vehicleRoutes.get(i).get(j).dummyServiceTime);
        } else{
            dummyServiceTime = Math.max(solution.vehicleRoutes.get(i).get(j).vehicleServiceTime + solution.vehicleRoutes.get(i).get(j).readyTime,
                    robotNode.readyTime + robotNode.robotServiceTime + solution.parameters.distance[robotNode.nodeNum]
                            [solution.vehicleRoutes.get(i).get(j).nodeNum] / solution.parameters.robotSpeed) - getLi(solution.vehicleRoutes.get(i).get(j), robotNode, solution);
        }
        if(solution.vehicleRoutes.get(i).get(j).dummyReadyTime != Double.MAX_VALUE) {
            dummyReadyTime = Math.max(solution.vehicleRoutes.get(i).get(j).dummyReadyTime, getLi(solution.vehicleRoutes.get(i).get(j), robotNode, solution));
        }else{
            dummyReadyTime = getLi(solution.vehicleRoutes.get(i).get(j), robotNode, solution);
        }
        res.add(dummyReadyTime);
        res.add(dummyServiceTime);
        res.add(dummyDueTime);
        return res;
    }
    public double getLi(Customer ci, Customer cj, Solution solution){
        //根据时间关系得到虚拟时间窗的下界
        //li:ci_ready ui:ci_due lj:cj_ready uj:cj_due
        double sij_d = (double) solution.parameters.distance[ci.nodeNum][cj.nodeNum] / solution.parameters.robotSpeed;
        if(cj.readyTime >= ci.readyTime){
            if(ci.vehicleServiceTime >= cj.robotServiceTime + 2 * sij_d && cj.readyTime + sij_d + cj.robotServiceTime - ci.vehicleServiceTime >= ci.dueTime) {
                return ci.dueTime;
            }
            else if(ci.vehicleServiceTime >= cj.robotServiceTime +
                    2 * sij_d && cj.readyTime + sij_d +
                            cj.robotServiceTime - ci.vehicleServiceTime < ci.dueTime && cj.readyTime + sij_d +
                    cj.robotServiceTime - ci.vehicleServiceTime > ci.readyTime){
                return cj.readyTime + sij_d + cj.robotServiceTime - ci.vehicleServiceTime;
            }
            else if(ci.vehicleServiceTime >= cj.robotServiceTime +
                    2 * sij_d && cj.readyTime + sij_d + cj.robotServiceTime - ci.vehicleServiceTime <= ci.readyTime){
                return ci.readyTime;
            }
            else if(ci.vehicleServiceTime < cj.robotServiceTime + 2 * sij_d && cj.readyTime - sij_d >= ci.dueTime){
                return ci.dueTime;
            }
            else if(ci.vehicleServiceTime < cj.robotServiceTime + 2 * sij_d && cj.readyTime - sij_d < ci.dueTime && cj.readyTime - sij_d > ci.readyTime){
                return cj.readyTime - sij_d;
            }
            else {
                return Math.max(cj.readyTime - sij_d, ci.readyTime + ci.vehicleServiceTime - cj.robotServiceTime - 2 * sij_d);
            }
        }
        else{
            if(ci.vehicleServiceTime < cj.robotServiceTime + 2 * sij_d){
                if(cj.dueTime - sij_d <= ci.readyTime + ci.vehicleServiceTime + cj.robotServiceTime - 2 * sij_d){
                    return cj.dueTime - sij_d;
                }else{
                    return ci.readyTime + ci.vehicleServiceTime - cj.robotServiceTime - 2 * sij_d;
                }
            }
            else{
                if(cj.dueTime - sij_d <= ci.readyTime) return cj.dueTime - sij_d;
                else return ci.readyTime;
            }
        }
    }
}
