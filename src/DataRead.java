import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class DataRead {
    //读取数据
    ArrayList<Customer> customers;
    Parameters parameters;
    public void read(int curNodes, int curNum) throws IOException {
        parameters = new Parameters();
        customers = new ArrayList<>();
        String curNum_str;
        if(curNum < 10) curNum_str = "0" + curNum;
        else curNum_str = String.valueOf(curNum);
        String fileName = "data\\data" + curNodes + "\\Cardiff"+curNodes+"_"+curNum_str+".txt";
        File file = new File(fileName);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        parameters.num_of_customers = Integer.parseInt(br.readLine());
        br.readLine();
        parameters.vehicleCapacity = Integer.parseInt(br.readLine());
        parameters.vehicleSpeed = Integer.parseInt(br.readLine());
        parameters.robotSpeed = Integer.parseInt(br.readLine());
        parameters.robotRadius = Integer.parseInt(br.readLine());
        br.readLine();
        parameters.distance = new int[parameters.num_of_customers+1][parameters.num_of_customers+1];
        parameters.removeMaxNum = (int) (0.15 * parameters.num_of_customers);
        parameters.removeMinNum = (int) (0.15 * parameters.num_of_customers);
        for(int i=0;i<parameters.num_of_customers+1;i++){
            line = br.readLine();
            String[] temp = line.split("\t");
            int[] temp1 = new int[temp.length];
            for(int j=0;j<temp.length;j++) {
                //if(i == j) parameters.distance[i][j] = Integer.MAX_VALUE;
                parameters.distance[i][j] = Integer.parseInt(temp[j]);
            }
        }
        br.readLine();
        br.readLine();
        for(int i=0;i<parameters.num_of_customers+1;i++){
            Customer customer = new Customer();
            line = br.readLine();
            if(Objects.equals(line, "")) {
                i--;
                continue;
            }
            String[] t1 = line.split(" ");
            ArrayList<String[]> arrayList = new ArrayList<>();
            for(int j=0;j< t1.length;j++){
                arrayList.add(t1[j].split("\t"));
            }
            int len = 0;
            for(int j=0;j<arrayList.size();j++){
                len += arrayList.get(j).length;
            }
            String[] temp1 = new String[len];
            int f = 0;
            len = 0;
            for(int j=0;j<arrayList.size();j++){
                for(int k=0;k<arrayList.get(j).length;k++){
                    temp1[f++] = arrayList.get(j)[k];
                    if(!Objects.equals(arrayList.get(j)[k], "")) len++;
                }
            }
            String[] temp = new String[len];
            f = 0;
            for(int j=0;j<temp1.length;j++){
                if(!Objects.equals(temp1[j], "")) temp[f++] = temp1[j];
            }
            try {
                customer.nodeNum = Integer.parseInt(temp[0].replace(" ", ""));
                customer.postCode = temp[1].replace(" ", "");
                customer.robotAccess = Integer.parseInt(temp[2].replace(" ", ""));
                customer.demand = Integer.parseInt(temp[3].replace(" ", ""));
                customer.readyTime = Integer.parseInt(temp[4].replace(" ", ""));
                customer.dueTime = Integer.parseInt(temp[5].replace(" ", ""));
                customer.vehicleServiceTime = Integer.parseInt(temp[6].replace(" ", ""));
                customer.robotServiceTime = Integer.parseInt(temp[7].replace(" ", ""));
                customers.add(customer);
            }catch (ArrayIndexOutOfBoundsException e){
                System.out.println();
            }
        }
    }

}
