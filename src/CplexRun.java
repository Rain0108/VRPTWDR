import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

public class CplexRun {
    //求解模型的精确解
    public static double solve(DataRead dataRead, Parameters parameters, int carNum) {
        try {
            //创建模型
            IloCplex cplex = new IloCplex();
            cplex.setParam(IloCplex.DoubleParam.TimeLimit, 3000);
            //决策变量
            IloNumVar[][][] x_ijk = new IloNumVar[parameters.num_of_customers+1][parameters.num_of_customers+1][carNum];
            IloNumVar[][][][] y_ijdk = new IloNumVar[parameters.num_of_customers][parameters.num_of_customers][parameters.robotNum][carNum];
            IloNumVar[][][] p_ijk = new IloNumVar[parameters.num_of_customers+1][parameters.num_of_customers+1][carNum];
            IloNumVar[] a_i = new IloNumVar[parameters.num_of_customers];
            IloNumVar[] b_i = new IloNumVar[parameters.num_of_customers];
            IloNumVar[] w_i = new IloNumVar[parameters.num_of_customers];
            for(int i=0;i< parameters.num_of_customers+1;i++){
                for(int j=0;j< parameters.num_of_customers+1;j++){
                    for(int k=0;k<carNum;k++){
                        x_ijk[i][j][k] = cplex.numVar(0.0, 1.0, IloNumVarType.Bool, "x"+i+" "+j+" "+k);
                        p_ijk[i][j][k] = cplex.numVar(0.0, Double.MAX_VALUE, IloNumVarType.Float, "p"+i+" "+j+" "+k);
                    }
                }
            }
            for(int i=0;i< parameters.num_of_customers;i++){
                for(int j=0;j< parameters.num_of_customers;j++){
                    for(int d=0;d< parameters.robotNum;d++) {
                        for (int k = 0; k < carNum; k++) {
                            y_ijdk[i][j][d][k] = cplex.numVar(0.0, 1.0, IloNumVarType.Bool, "y"+i+" "+j+" "+d+" "+k);
                        }
                    }
                }
            }
            for(int i=0;i< parameters.num_of_customers;i++){
                a_i[i] = cplex.numVar(0.0, Double.MAX_VALUE, IloNumVarType.Float, "a"+(i+1));
                b_i[i] = cplex.numVar(0.0, Double.MAX_VALUE, IloNumVarType.Float, "b"+(i+1));
                w_i[i] = cplex.numVar(0.0, Double.MAX_VALUE, IloNumVarType.Float, "w"+(i+1));
            }
            //设置目标函数
            int[][] dis = new int[parameters.num_of_customers+1][parameters.num_of_customers+1];
            for(int i=0;i< parameters.num_of_customers+1;i++){
                for(int j=0;j< parameters.num_of_customers+1;j++){
                    if(i == j) dis[i][j] = 9999999;
                    else dis[i][j] = parameters.distance[i][j];
                }
            }
            double[][] s_ijv = new double[parameters.num_of_customers+1][parameters.num_of_customers+1];
            for(int i=0;i< parameters.num_of_customers+1;i++){
                for(int j=0;j< parameters.num_of_customers+1;j++){
                    s_ijv[i][j] = (double) dis[i][j] / parameters.vehicleSpeed;
                }
            }
            double[][] s_ijd = new double[parameters.num_of_customers+1][parameters.num_of_customers+1];
            for(int i=0;i< parameters.num_of_customers+1;i++){
                for(int j=0;j< parameters.num_of_customers+1;j++){
                    s_ijd[i][j] = (double) dis[i][j] / parameters.robotSpeed;
                }
            }

            IloNumExpr obj = cplex.numExpr();
            for(int i=0;i< parameters.num_of_customers+1;i++){
                for(int j=0;j< parameters.num_of_customers+1;j++){
                    for(int k=0;k<carNum;k++){
                        IloNumExpr subExpr = cplex.prod(s_ijv[i][j], x_ijk[i][j][k]);
                        obj = cplex.sum(obj, subExpr);
                    }
                }
            }
            for(int i=0;i< parameters.num_of_customers;i++){
                IloNumExpr subExpr = cplex.diff(b_i[i], a_i[i]);
                subExpr = cplex.sum(subExpr, w_i[i]);
                obj = cplex.sum(obj, subExpr);
            }
            cplex.addMinimize(obj);
            //设置约束
            int curNum = 0;
            for(int j=0;j< parameters.num_of_customers;j++){
                IloNumExpr n1 = cplex.numExpr();
                for(int i=0;i< parameters.num_of_customers+1;i++){
                    for(int k=0;k<carNum;k++){
                        n1 = cplex.sum(n1, x_ijk[i][j+1][k]);
                    }
                }
                for(int i=0;i< parameters.num_of_customers;i++){
                    for(int d=0;d<parameters.robotNum;d++) {
                        for (int k = 0; k < carNum; k++) {
                            n1 = cplex.sum(n1, y_ijdk[i][j][d][k]);
                        }
                    }
                }
                cplex.addEq(n1, 1);
                curNum++;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int j=0;j< parameters.num_of_customers;j++){
                IloNumExpr n2 = cplex.numExpr();
                for(int i=0;i< parameters.num_of_customers;i++){
                    for(int d=0;d<parameters.robotNum;d++) {
                        for (int k = 0; k < carNum; k++) {
                            n2 = cplex.sum(n2, y_ijdk[i][j][d][k]);
                        }
                    }
                }
                cplex.addLe(n2, dataRead.customers.get(j+1).robotAccess);
                curNum++;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i< parameters.num_of_customers;i++){
                for (int k = 0; k < carNum; k++) {
                    IloNumExpr n3 = cplex.numExpr();
                    for(int j=0;j<parameters.num_of_customers;j++){
                        for(int d=0;d< parameters.robotNum;d++){
                            n3 = cplex.sum(n3, y_ijdk[i][j][d][k]);
                        }
                    }
                    IloNumExpr n_temp = cplex.numExpr();
                    for(int j=0;j<parameters.num_of_customers+1;j++){
                        n_temp = cplex.sum(n_temp, x_ijk[j][i+1][k]);
                    }
                    n_temp = cplex.prod(n_temp, parameters.robotNum);
                    n3 = cplex.diff(n3 ,n_temp);
                    cplex.addLe(n3, 0);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i< parameters.num_of_customers;i++){
                for (int d = 0; d < parameters.robotNum; d++) {
                    IloNumExpr n4 = cplex.numExpr();
                    for(int j=0;j<parameters.num_of_customers;j++){
                        for(int k=0;k<carNum;k++){
                            n4 = cplex.sum(n4, y_ijdk[i][j][d][k]);
                        }
                    }
                    cplex.addLe(n4, 1);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int k=0;k<carNum;k++){
                IloNumExpr n5 = cplex.numExpr();
                for(int j=0;j< parameters.num_of_customers;j++){
                    n5 = cplex.sum(n5, x_ijk[0][j+1][k]);
                }
                cplex.addLe(n5, 1);
                curNum ++;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int j=0;j<parameters.num_of_customers+1;j++){
                for(int k=0;k<carNum;k++){
                    IloNumExpr n6 = cplex.numExpr();
                    for(int i=0;i<parameters.num_of_customers+1;i++){
                        n6 = cplex.sum(n6, x_ijk[i][j][k]);
                        n6 = cplex.diff(n6, x_ijk[j][i][k]);
                    }
                    cplex.addEq(n6, 0);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int j=0;j<parameters.num_of_customers;j++) {
                IloNumExpr n7 = cplex.numExpr();
                IloNumExpr n7_1 = cplex.numExpr();
                for (int i = 0; i < parameters.num_of_customers + 1; i++) {
                    for (int k = 0; k < carNum; k++) {
                        n7 = cplex.sum(n7, p_ijk[i][j+1][k]);
                        n7 = cplex.diff(n7, p_ijk[j+1][i][k]);
                        n7_1 = cplex.sum(n7_1, x_ijk[i][j+1][k]);
                    }
                }
                n7_1 = cplex.prod(n7_1, dataRead.customers.get(j+1).demand);
                for(int i=0;i<parameters.num_of_customers;i++){
                    for(int k=0;k<carNum;k++){
                        for(int d=0;d<parameters.robotNum;d++){
                            IloNumExpr n7_temp = cplex.prod(dataRead.customers.get(i+1).demand, y_ijdk[j][i][d][k]);
                            n7_1 = cplex.sum(n7_1, n7_temp);
                        }
                    }
                }
                n7 = cplex.diff(n7, n7_1);
                cplex.addEq(n7, 0);
                curNum++;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i< parameters.num_of_customers;i++){
                for(int j=0;j< parameters.num_of_customers+1;j++){
                    for(int k=0;k<carNum;k++){
                        IloNumExpr n8 = cplex.numExpr();
                        for(int t=0;t<parameters.num_of_customers;t++){
                            for(int d=0;d<parameters.robotNum;d++){
                                IloNumExpr iloNumExpr = cplex.prod(dataRead.customers.get(i+1).demand, y_ijdk[i][t][d][k]);
                                n8 = cplex.sum(n8, iloNumExpr);
                            }
                        }
                        n8 = cplex.diff(parameters.vehicleCapacity - dataRead.customers.get(i+1).demand, n8);
                        n8 = cplex.prod(n8, x_ijk[i+1][j][k]);
                        n8 = cplex.diff(p_ijk[i+1][j][k], n8);
                        cplex.addLe(n8, 0);
                        curNum++;
                    }
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i<parameters.num_of_customers;i++){
                for(int j=0;j<parameters.num_of_customers;j++){
                    IloNumExpr n9 = cplex.numExpr();
                    n9 = cplex.sum(n9, b_i[i]);
                    n9 = cplex.diff(n9, a_i[j]);
                    n9 = cplex.sum(n9, w_i[i]);
                    n9 = cplex.sum(n9, s_ijv[i+1][j+1]);
                    IloNumExpr n9_1 = cplex.numExpr();
                    for(int k=0;k<carNum;k++){
                        n9_1 = cplex.sum(n9_1, x_ijk[i+1][j+1][k]);
                    }
                    n9_1 = cplex.diff(1, n9_1);
                    n9_1 = cplex.prod(9999999, n9_1);
                    cplex.addLe(n9, n9_1);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i<parameters.num_of_customers;i++){
                for(int j=0;j<parameters.num_of_customers;j++){
                    IloNumExpr n10 = cplex.numExpr();
                    n10 = cplex.sum(n10, a_i[j]);
                    n10 = cplex.diff(n10, b_i[i]);
                    n10 = cplex.diff(n10, w_i[i]);
                    n10 = cplex.diff(n10, s_ijv[i+1][j+1]);
                    IloNumExpr n10_1 = cplex.numExpr();
                    for(int k=0;k<carNum;k++){
                        n10_1 = cplex.sum(n10_1, x_ijk[i+1][j+1][k]);
                    }
                    n10_1 = cplex.diff(1, n10_1);
                    n10_1 = cplex.prod(9999999, n10_1);
                    cplex.addLe(n10, n10_1);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i<parameters.num_of_customers;i++){
                for(int j=0;j<parameters.num_of_customers;j++){
                    IloNumExpr n11 = cplex.numExpr();
                    n11 = cplex.sum(n11, a_i[i]);
                    n11 = cplex.diff(n11, a_i[j]);
                    n11 = cplex.sum(n11, s_ijd[i+1][j+1]);
                    IloNumExpr n11_1 = cplex.numExpr();
                    for(int k=0;k<carNum;k++){
                        for(int d=0;d< parameters.robotNum;d++) {
                            n11_1 = cplex.sum(n11_1, y_ijdk[i][j][d][k]);
                        }
                    }
                    n11_1 = cplex.diff(1, n11_1);
                    n11_1 = cplex.prod(9999999, n11_1);
                    cplex.addLe(n11, n11_1);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i< parameters.num_of_customers;i++){
                IloNumExpr n12 = cplex.numExpr();
                n12 = cplex.sum(n12, w_i[i]);
                IloNumExpr n12_1 = cplex.numExpr();
                for(int j=0;j<parameters.num_of_customers+1;j++){
                    for(int k=0;k<carNum;k++){
                        n12_1 = cplex.sum(n12_1, x_ijk[j][i+1][k]);
                    }
                }
                n12_1 = cplex.prod(n12_1, dataRead.customers.get(i+1).vehicleServiceTime);
                n12 = cplex.diff(n12, n12_1);
                cplex.addGe(n12, 0);
                curNum++;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i<parameters.num_of_customers;i++){
                for(int j=0;j<parameters.num_of_customers;j++){
                    IloNumExpr n13 = cplex.numExpr();
                    n13 = cplex.sum(n13, b_i[j]);
                    n13 = cplex.diff(n13, b_i[i]);
                    n13 = cplex.sum(n13, dataRead.customers.get(j+1).vehicleServiceTime);
                    n13 = cplex.sum(n13, s_ijd[j+1][i+1]);
                    n13 = cplex.diff(n13, w_i[i]);
                    IloNumExpr n13_1 = cplex.numExpr();
                    for(int k=0;k<carNum;k++){
                        for(int d=0;d< parameters.robotNum;d++) {
                            n13_1 = cplex.sum(n13_1, y_ijdk[i][j][d][k]);
                        }
                    }
                    n13_1 = cplex.diff(1, n13_1);
                    n13_1 = cplex.prod(9999999, n13_1);
                    cplex.addLe(n13, n13_1);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i<parameters.num_of_customers;i++){
                IloNumExpr n14 = cplex.diff(a_i[i], b_i[i]);
                cplex.addLe(n14, 0);
                curNum++;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int i=0;i<parameters.num_of_customers;i++){
                cplex.addLe(b_i[i], dataRead.customers.get(i+1).dueTime);
                cplex.addGe(b_i[i], dataRead.customers.get(i+1).readyTime);
                curNum+=2;
            }
            System.out.println(curNum);
            curNum = 0;
            for(int k=0;k<carNum;k++){
                for(int d=0;d<parameters.robotNum;d++){
                    IloNumExpr n15 = cplex.numExpr();
                    for(int i=0;i< parameters.num_of_customers;i++){
                        for(int j=0;j<parameters.num_of_customers;j++){
                            IloNumExpr n15_1 = cplex.prod(dis[i+1][j+1], y_ijdk[i][j][d][k]);
                            n15 = cplex.sum(n15, n15_1);
                        }
                    }
                    cplex.addLe(n15, parameters.robotRadius);
                    curNum++;
                }
            }
            System.out.println(curNum);
            curNum = 0;
            //模型求解及输出
            if (cplex.solve()) {
                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value = " + cplex.getObjValue());
                cplex.exportModel("ex1.lp");
                double[][][] res = new double[x_ijk.length][x_ijk[0].length][x_ijk[0][0].length];
                double[][][][] res2 = new double[y_ijdk.length][y_ijdk[0].length][y_ijdk[0][0].length][y_ijdk[0][0][0].length];
                double[] res_a = new double[parameters.num_of_customers];
                double[] res_b = new double[parameters.num_of_customers];
                double[] res_w = new double[parameters.num_of_customers];
                for(int i=0;i< parameters.num_of_customers+1;i++){
                    for(int j=0;j< parameters.num_of_customers+1;j++){
                        for(int k=0;k<carNum;k++){
                            res[i][j][k] = cplex.getValue(x_ijk[i][j][k]);
                        }
                    }
                }
                for(int i=0;i< parameters.num_of_customers;i++){
                    for(int j=0;j< parameters.num_of_customers;j++){
                        for(int d=0;d< parameters.robotNum;d++){
                            for(int k=0;k<carNum;k++) {
                                res2[i][j][d][k] = cplex.getValue(y_ijdk[i][j][d][k]);
                            }
                        }
                    }
                }
                for(int i=0;i< parameters.num_of_customers;i++){
                    res_a[i] = cplex.getValue(a_i[i]);
                    res_b[i] = cplex.getValue(b_i[i]);
                    res_w[i] = cplex.getValue(w_i[i]);
                }
                System.out.println();
                ArrayList<Integer> nums = new ArrayList<>();
                for(int u=0;u<15;u++) {
                    int num = 0;
                    for (int i = 0; i < parameters.num_of_customers + 1; i++) {
                        for (int k = 0; k < carNum; k++) {
                            num += res[i][1][k];
                        }
                    }
                    for (int i = 0; i < parameters.num_of_customers; i++) {
                        for (int d = 0; d < parameters.robotNum; d++) {
                            for (int k = 0; k < carNum; k++) {
                                num += res2[i][1][d][k];
                            }
                        }
                    }
                    nums.add(num);
                }
                for(Integer i : nums) System.out.println(i);
                for(int i=0;i< parameters.num_of_customers;i++){
                    for(int j=0;j< parameters.num_of_customers;j++){
                        double temp = res_b[i] - res_a[j] + res_w[i] + s_ijv[i+1][j+1];
                        double x = 0;
                        for(int k=0;k<carNum;k++){
                            x += res[i+1][j+1][k];
                        }
                        boolean t = temp <= (9999999*(1-x));
                        if(!t)
                            System.out.println();
                        System.out.println(t);
                    }
                }
                return cplex.getObjValue();


            }
            cplex.end();


        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
        return -1;
    }
}
