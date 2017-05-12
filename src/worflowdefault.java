import java.text.DecimalFormat;
import java.util.*;
import java.lang.Math;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.provisioners.*;

public class worflowdefault{
	private static List<Cloudlet> cloudletList;  //tasks lists
	
	private static int cloudletNum=80;	//total number of tasks

	private static List<Vm> vmList;	//Virtual machine lists
	private static int vmNum=10; //total number of vms
	

	public static void main(String[] args){
		Log.printLine("startig test workflow");
		try{
			int num_user=1;
			Calendar calendar=Calendar.getInstance();
			boolean trace_flag=false;
			//step1 initialize cloudsim packet
			CloudSim.init(num_user, calendar, trace_flag);
			
			//step2: creating datacenter
			Datacenter datacenter0=createDatacenter("Datacenter_0");
			
			//step3:creating dacenter agent
			DatacenterBroker broker=createBroker();
			int brokerId=broker.getId();
			
			//seting vm parameters
			int vmid=0;
			int[] mipss=new int[]{256,410,400,319,452,190,320,210,421,400};
			long size=10000;
			int ram=2048;
			long bw=1000;
			int pesNumber=1;
			String vmm="Xen";
			
			//step4:cretating vm
			vmList=new ArrayList<Vm>();
			for(int i=0;i<vmNum;i++)
			{
				vmList.add(new Vm(vmid,brokerId,mipss[i],pesNumber,ram,bw,size,vmm,new CloudletSchedulerSpaceShared()));
				vmid++;
			}
			// dilivering vm lists
			broker.submitVmList(vmList);
			
			//task parameters
			int id=0;
			
			long length = 40000;
			long fileSize=300;
			long outputSize=300;
			UtilizationModel utilizationModel=new UtilizationModelFull();
			
			
			//step5:creating cloud lists
			cloudletList=new ArrayList<Cloudlet>();
		
			for(int i=0;i<cloudletNum;i++)
			{
				Cloudlet cloudlet=new Cloudlet(id,length,pesNumber,
						fileSize,outputSize,utilizationModel,utilizationModel,utilizationModel);
				
				cloudlet.setUserId(brokerId);
				//cloudlet.setDeepth();
				cloudletList.add(cloudlet);
				id++;
			}
			LinkedList<Storage> storageList=new LinkedList <Storage>();//addFile(List<File> list)Adds a set of files to the storage 
			
			for(int i=0;i<cloudletNum;i++)
			{////HarddriveStorage仿真硬盘驱动器存储(String name, double capacity）
				HarddriveStorage str=new HarddriveStorage(Integer.toString(cloudletList.get(i).getCloudletId()),300000);
				storageList.add(str);
			}
			for(int i=1;i<=cloudletNum-1;i++)
			{
				Random random=new Random();
			    int top= random.nextInt(5)%(5-1+1) +1;//随机设置节点接受的文件个数
			    for(int j=0;j<=top;j++)
			    {
			    int a=random.nextInt(cloudletNum-2)%((cloudletNum-2)-0+1) +0;
				File file=new File
				(Integer.toString(cloudletList.get(a).getCloudletId()),(int)cloudletList.get(a).getCloudletOutputSize());
				storageList.get(i).addFile(file);
				cloudletList.get(i).addRequiredFile(Integer.toString(cloudletList.get(a).getCloudletId()));
			    }
			  /*  if (cloudletList.get(i).getRequiredFiles().size()==0)//若该节点不接受任何文件则将节点一的输出文件加入
			    {
			    	File file=new File
					(Integer.toString(cloudletList.get(0).getCloudletId()),(int)cloudletList.get(0).getCloudletOutputSize());
					storageList.get(i).addFile(file);
					cloudletList.get(i).addRequiredFile(Integer.toString(cloudletList.get(0).getCloudletId()));
			    }*/
			}
			for(int i=1;i<cloudletNum-1;i++)
			{
			    int count=0;
			    for(int k=1;k<cloudletNum-1;k++)
			    {
			    	if(!cloudletList.get(k).getRequiredFiles().contains(Integer.toString(cloudletList.get(i).getCloudletId())))
			    	{
			    		count++;
			    	}
			    }
			   if(count==cloudletNum-2)
			   {
				   File file=new File
				   (Integer.toString(cloudletList.get(i).getCloudletId()),(int)cloudletList.get(i).getCloudletOutputSize());
					storageList.get(cloudletNum-1).addFile(file);
					cloudletList.get(cloudletNum-1).addRequiredFile(Integer.toString(cloudletList.get(i).getCloudletId()));
			   }
			   
			}//终结点全部把输出文件给cloudletNum-1节点
			/* for(int i=1;i<cloudletNum-1;i++)
				{
				   if(!cloudletList.get(i).getRequiredFiles().contains(Integer.toString(cloudletList.get(i).getCloudletId())))
				   {
					   File file=new File
						(Integer.toString(cloudletList.get(0).getCloudletId()),(int)cloudletList.get(0).getCloudletOutputSize());
						storageList.get(i).addFile(file);
						cloudletList.get(i).addRequiredFile(Integer.toString(cloudletList.get(0).getCloudletId()));
				   }
				}*/
			Random random=new Random();
		    int top= random.nextInt(5)%(5-1+1) +1;//随机设置节点接受的文件个数
		    for(int j=0;j<=top;j++)
		    {
		    int x=random.nextInt(cloudletNum-2)%((cloudletNum-2)-0+1) +0;
			File file=new File
			(Integer.toString(cloudletList.get(0).getCloudletId()),(int)cloudletList.get(0).getCloudletOutputSize());
			storageList.get(x).addFile(file);
			cloudletList.get(x).addRequiredFile(Integer.toString(cloudletList.get(0).getCloudletId()));
		    }
		    for(int i=0;i<=cloudletNum-1;i++)
		    	for(int j=0;j<=cloudletNum-1;j++)
		    	{
		    		if(cloudletList.get(i).getRequiredFiles().contains(Integer.toString(cloudletList.get(j).getCloudletId()))&&cloudletList.get(j).getRequiredFiles().contains(Integer.toString(cloudletList.get(i).getCloudletId())))
			    	{
		    			
					    int top1= random.nextInt(5)%(5-1+1) +1;//随机设置节点接受的文件个数
					    for(int D=0;j<=top1;j++)
					    {
					    int a=random.nextInt(cloudletNum-2)%((cloudletNum-2)-0+1) +0;
						File file=new File
						(Integer.toString(cloudletList.get(a).getCloudletId()),(int)cloudletList.get(a).getCloudletOutputSize());
						storageList.get(i).addFile(file);
						cloudletList.get(i).addRequiredFile(Integer.toString(cloudletList.get(a).getCloudletId()));
					    }
			    	}
			    }
			
			datacenter0.setStorageList(storageList);
			broker.submitCloudletList(cloudletList);
			//绑定任务到虚拟机
			
			
	
			//broker.bindCloudletsToVmsSimple();
			//broker.bindPDTsToVmbyRACO();
			broker.bindPDTsToVmbyLBRACO();

			
			
			//step7: start simulation
			CloudSim.startSimulation();
			List<Cloudlet> newList=broker.getCloudletReceivedList();
			CloudSim.stopSimulation();
			printCloudletList(newList);
			datacenter0.printDebts();
			Log.printLine("PDTsData finished");
			CloudSim.init(num_user, calendar, trace_flag);


		
		}
		catch(Exception e){
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
	}


//Datacenter中创建数据中心函数
private static Datacenter createDatacenter(String name)
{
	//step1:creating host lists
	List<Host> hostList=new ArrayList<Host>();
	//PE and host parameters
	int mips=1000;
	int hostId=0;
	int ram=2048;
	long storage=1000000;
	int bw=10000;
	for(int i=0;i<vmNum;i++){
		//step2:creating PE List
		List<Pe> peList=new ArrayList<Pe>();
		//step3:creating pe and add them to pe list
		peList.add(new Pe(0,new PeProvisionerSimple(mips)));
		//step4:creating hosts and add them to the host list
		hostList.add(
				new Host(
						hostId,
						new RamProvisionerSimple(ram),
						new BwProvisionerSimple(bw),
						storage,
						peList,
						new VmSchedulerTimeShared(peList)));
		hostId++;
	}
	//DataCenter features
	String arch="x86";
	String os="Linux";
	String vmm="Xen";
	double time_zone=10.0;
	double cost=3.0;
	double costPerMem=0.05;
	double costPerStorage=0.001;
	double costPerBw=0.15;
	LinkedList<Storage> storageList=new LinkedList<Storage>();
	//step5:creating datacenter feature objects
	DatacenterCharacteristics characteristics=new DatacenterCharacteristics(arch,os,vmm,hostList,time_zone,cost,costPerMem,costPerStorage,costPerBw);
	//creating datacenter objects
	Datacenter datacenter=null;
	try{
		datacenter=new Datacenter(name,characteristics,new VmAllocationPolicySimple(hostList),storageList,0);
		
	}catch(Exception e){
		e.printStackTrace();
	}
	return datacenter;
	}

//creating datacenter agent
private static DatacenterBroker createBroker(){
	DatacenterBroker broker=null;
	try{
		broker=new DatacenterBroker("Broker");
	}catch(Exception e){
		e.printStackTrace();
		return null;
	}
	return broker;
	}
//OutPut Statistics imformation

private static void printCloudletList(List<Cloudlet> list){
	int size = list.size();
	Cloudlet cloudlet;

	String indent = "    ";
	Log.printLine();
	Log.printLine("========== OUTPUT ==========");
	Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
			"Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

	DecimalFormat dft = new DecimalFormat("###.##");
	for (int i = 0; i < size; i++) {
		cloudlet = list.get(i);
		Log.print(indent + cloudlet.getCloudletId() + indent + indent);

		if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
			Log.print("SUCCESS");

			Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
					indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
					indent + indent + dft.format(cloudlet.getFinishTime()));
		}
	}
	System.out.println();
	System.out.println("***********************");	
	for (int i = 0; i < size; i++) {
		cloudlet = list.get(i);
		if(cloudlet.getCloudletStatus()==Cloudlet.SUCCESS)
			System.out.println(dft.format(cloudlet.getFinishTime()));
	}
	System.out.println("***********************");	
	for (int i = 0; i < size; i++) {
		cloudlet = list.get(i);
		if(cloudlet.getCloudletStatus()==Cloudlet.SUCCESS)
			System.out.println(dft.format(cloudlet.getCloudletId()));
	}
	
	
}

}