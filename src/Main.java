import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws IOException {
        double[] iniv = new double[20];  //初始解的cost
        double[] finv = new double[20];  //最优解的cost
        int[] ini_c = new int[20];  //初始解的用车数量
        int[] fin_c = new int[20];  //最优解的用车数量
        for(int i=1;i<=20;i++) {
            DataRead dataRead = new DataRead();
            dataRead.read(50, i);
            //double x = CplexRun.solve(dataRead, dataRead.parameters, 6);
            double curTime = System.currentTimeMillis();
            Solution solution = new Solution();
            solution.initialSolution(dataRead);
            //System.out.println(Algorithm.solutionFeasible(solution));
            double ini = solution.totalCost;
            int ini_carNum = solution.vehicleRoutes.size();
            ALNS alns = new ALNS(dataRead);
            Solution solution1 = alns.ALNS_run(solution);
            double endTime = System.currentTimeMillis();
            System.out.println(ini);
            iniv[i-1] = ini;
            System.out.println(Algorithm.solutionFeasible(solution1));
            System.out.println(Algorithm.getSolutionCost(solution1));
            System.out.println(ini_carNum);
            System.out.println((endTime - curTime) / 1000);
            ini_c[i-1] = ini_carNum;
            finv[i-1] = Algorithm.getSolutionCost(solution1);
            System.out.println(solution1.vehicleRoutes.size());
            fin_c[i-1] = solution1.vehicleRoutes.size();
            //System.out.println(x);
        }
        for(int i=0;i<20;i++){
            System.out.print(iniv[i]);
            System.out.print(",");
        }
        System.out.println();
        for(int i=0;i<20;i++){
            System.out.print(finv[i]);
            System.out.print(",");
        }
        System.out.println();
        for(int i=0;i<20;i++){
            System.out.print(ini_c[i]);
            System.out.print(",");
        }
        System.out.println();
        for(int i=0;i<20;i++){
            System.out.print(fin_c[i]);
            System.out.print(",");
        }
    }
}