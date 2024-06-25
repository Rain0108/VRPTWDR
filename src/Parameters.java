import java.util.Random;

public class Parameters {
    public int[][] distance;  //距离矩阵
    public int num_of_customers;  //客户总数（不含仓库）
    public int vehicleCapacity;  //车辆核载
    public int vehicleSpeed;  //车辆速度
    public int robotSpeed;  //机器人速度
    public int robotRadius;  //机器人最大服务半径
    public int robotNum;  //每辆车配备的机器人数量
    public Random random;
    public int removeMaxNum;  //移除客户点的最大数量
    public int removeMinNum;  //移除客户点的最小数量
    public double removeNumDecSpeed = 0.3;  //移除客户点数量的下降速度
    public double temperDecSpeed = 0.3;  //模拟退火的温度下降速度
    public double weightRenewSpeed = 0.5;  //算子权重的更新速度
    public double T = 100;  //最高温度
    public double minT = 10;  //最低温度
    public int maxIter = 1000;  //最大迭代次数
    public Parameters(int num_of_customers, int vehicleCapacity, int vehicleSpeed, int robotSpeed, int robotRadius){
        this.num_of_customers = num_of_customers;
        this.vehicleCapacity = vehicleCapacity;
        this.vehicleSpeed = vehicleSpeed;
        this.robotSpeed = robotSpeed;
        this.robotRadius = robotRadius;
        this.robotNum = 4;
        this.random = new Random();
    }
    public Parameters(){
        this.robotNum = 4;
        this.random = new Random();
    }
}
class Customer{
    int nodeNum;  //节点编号
    String postCode;  //邮编
    int robotAccess;  //机器人可达性，1为可达，0为不可达
    int demand;  //需求
    int readyTime;  //最早开始服务时间
    int dueTime;  //最晚开始服务时间
    int vehicleServiceTime;  //货车服务时间
    int robotServiceTime;  //机器人服务时间
    double dummyServiceTime;  //虚拟的货车服务时间
    double dummyReadyTime;  //虚拟的最早开始服务时间
    double dummyDueTime;  //虚拟的最晚开始服务时间
    public Customer(int nodeNum, String postCode, int robotAccess, int readyTime, int dueTime, int demand, int vehicleServiceTime, int robotServiceTime){
        this.nodeNum = nodeNum;
        this.postCode = postCode;
        this.robotAccess = robotAccess;
        this.readyTime = readyTime;
        this.dueTime = dueTime;
        this.demand = demand;
        this.vehicleServiceTime = vehicleServiceTime;
        this.robotServiceTime = robotServiceTime;
        this.dummyDueTime = Double.MAX_VALUE;
        this.dummyReadyTime = Double.MAX_VALUE;
        this.dummyServiceTime = Double.MAX_VALUE;
    }
    public Customer(){
        this.dummyDueTime = Double.MAX_VALUE;
        this.dummyReadyTime = Double.MAX_VALUE;
        this.dummyServiceTime = Double.MAX_VALUE;
    }
    public static Customer customerClone(Customer customer){
        Customer customer1 = new Customer();
        customer1.nodeNum = customer.nodeNum;
        customer1.robotAccess = customer.robotAccess;
        customer1.postCode = customer.postCode;
        customer1.demand = customer.demand;
        customer1.readyTime = customer.readyTime;
        customer1.dueTime = customer.dueTime;
        customer1.vehicleServiceTime = customer.vehicleServiceTime;
        customer1.robotServiceTime = customer.robotServiceTime;
        customer1.dummyServiceTime = customer.dummyServiceTime;
        customer1.dummyReadyTime = customer.dummyReadyTime;
        customer1.dummyDueTime = customer.dummyDueTime;
        return customer1;
    }
}
