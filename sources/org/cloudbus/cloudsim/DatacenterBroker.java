/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2010, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * DatacentreBroker represents a broker
 * acting on behalf of a user. It hides VM management,
 * as vm creation, sumbission of cloudlets to this VMs
 * and destruction of VMs.
 *
 * @author		Rodrigo N. Calheiros
 * @author		Anton Beloglazov
 * @since		CloudSim Toolkit 1.0
 */
public class DatacenterBroker extends SimEntity {

	// TODO: remove unnecessary variables

	/** The vm list. */
	private List<? extends Vm> vmList;

	/** The vms created list. */
	private List<? extends Vm> vmsCreatedList;

	/** The cloudlet list. */
	private List<? extends Cloudlet> cloudletList;

	/** The cloudlet submitted list. */
	private List<? extends Cloudlet> cloudletSubmittedList;

	/** The cloudlet received list. */
	private List<? extends Cloudlet> cloudletReceivedList;

	/** The cloudlets submitted. */
	private int cloudletsSubmitted;

	/** The vms requested. */
	private int vmsRequested;

	/** The vms acks. */
	private int vmsAcks;

	/** The vms destroyed. */
	private int vmsDestroyed;

	/** The datacenter ids list. */
	private List<Integer> datacenterIdsList;

	/** The datacenter requested ids list. */
	private List<Integer> datacenterRequestedIdsList;

	/** The vms to datacenters map. */
	private Map<Integer, Integer> vmsToDatacentersMap;

	/** The datacenter characteristics list. */
	private Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;


	/**
	 * Created a new DatacenterBroker object.
	 *
	 * @param name 	name to be associated with this entity (as
	 * required by Sim_entity class from simjava package)
	 *
	 * @throws Exception the exception
	 *
	 * @pre name != null
	 * @post $none
	 */
	public DatacenterBroker(String name) throws Exception {
		super(name);

		setVmList(new ArrayList<Vm>());
		setVmsCreatedList(new ArrayList<Vm>());
		setCloudletList(new ArrayList<Cloudlet>());
		setCloudletSubmittedList(new ArrayList<Cloudlet>());
		setCloudletReceivedList(new ArrayList<Cloudlet>());

		cloudletsSubmitted=0;
		setVmsRequested(0);
		setVmsAcks(0);
		setVmsDestroyed(0);

		setDatacenterIdsList(new LinkedList<Integer>());
		setDatacenterRequestedIdsList(new ArrayList<Integer>());
		setVmsToDatacentersMap(new HashMap<Integer, Integer>());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
	}

	/**
	 * This method is used to send to the broker the list with
	 * virtual machines that must be created.
	 *
	 * @param list the list
	 *
	 * @pre list !=null
	 * @post $none
	 */
	public void submitVmList(List<? extends Vm> list) {
		getVmList().addAll(list);
	}

	/**
	 * This method is used to send to the broker the list of
	 * cloudlets.
	 *
	 * @param list the list
	 *
	 * @pre list !=null
	 * @post $none
	 */
	public void submitCloudletList(List<? extends Cloudlet> list){
		getCloudletList().addAll(list);
	}

	/**
	 * Specifies that a given cloudlet must run in a specific virtual machine.
	 *
	 * @param cloudletId ID of the cloudlet being bount to a vm
	 * @param vmId the vm id
	 *
	 * @pre cloudletId > 0
	 * @pre id > 0
	 * @post $none
	 */
	public void bindCloudletToVm(int cloudletId, int vmId){
		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
	}

    /**
     * Processes events available for this Broker.
     *
     * @param ev    a SimEvent object
     *
     * @pre ev != null
     * @post $none
     */
	@Override
	public void processEvent(SimEvent ev) {
		//Log.printLine(CloudSim.clock()+"[Broker]: event received:"+ev.getTag());
		switch (ev.getTag()){
			// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);
				break;
			// Resource characteristics answer
	        case CloudSimTags.RESOURCE_CHARACTERISTICS:
	        	processResourceCharacteristics(ev);
	            break;
	        // VM Creation answer
	        case CloudSimTags.VM_CREATE_ACK:
	           	processVmCreate(ev);
	           	break;
	        //A finished cloudlet returned
	        case CloudSimTags.CLOUDLET_RETURN:
	        	processCloudletReturn(ev);
	            break;
	        // if the simulation finishes
	        case CloudSimTags.END_OF_SIMULATION:
	        	shutdownEntity();
	            break;
            // other unknown tags are processed by this method
	        default:
	            processOtherEvent(ev);
	            break;
		}
	}

	/**
	 * Process the return of a request for the characteristics of a PowerDatacenter.
	 *
	 * @param ev a SimEvent object
	 *
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter(getDatacenterIdsList().get(0));
		}
	}

	/**
	 * Process a request for the characteristics of a PowerDatacenter.
	 *
	 * @param ev a SimEvent object
	 *
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterIdsList(CloudSim.getCloudResourceList());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

		Log.printLine(CloudSim.clock()+": "+getName()+ ": Cloud Resource List received with "+getDatacenterIdsList().size()+" resource(s)");

		for (Integer datacenterId : getDatacenterIdsList()) {
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
		}
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 *
	 * @param ev a SimEvent object
	 *
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printLine(CloudSim.clock()+": "+getName()+ ": VM #"+vmId+" has been created in Datacenter #" + datacenterId + ", Host #" + VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
		} else {
			Log.printLine(CloudSim.clock()+": "+getName()+ ": Creation of VM #"+vmId+" failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();

		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) { // all the requested VMs have been created
			submitCloudlets();
		} else {
			if (getVmsRequested() == getVmsAcks()) { // all the acks received, but some VMs were not created
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				//all datacenters already queried
				if (getVmsCreatedList().size() > 0) { //if some vm were created
					submitCloudlets();
				} else { //no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName() + ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}

	/**
	 * Process a cloudlet return event.
	 *
	 * @param ev a SimEvent object
	 *
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printLine(CloudSim.clock()+": "+getName()+ ": Cloudlet "+cloudlet.getCloudletId()+" received");
		cloudletsSubmitted--;
		if (getCloudletList().size()==0&&cloudletsSubmitted==0) { //all cloudlets executed
			Log.printLine(CloudSim.clock()+": "+getName()+ ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { //some cloudlets haven't finished yet
			if (getCloudletList().size()>0 && cloudletsSubmitted==0) {
				//all the cloudlets sent finished. It means that some bount
				//cloudlet is waiting its VM be created
				clearDatacenters();
				createVmsInDatacenter(0);
			}

		}
	}

	/**
	 * Overrides this method when making a new and different type of Broker.
	 * This method is called by {@link #body()} for incoming unknown tags.
	 *
	 * @param ev   a SimEvent object
	 *
	 * @pre ev != null
	 * @post $none
	 */
    protected void processOtherEvent(SimEvent ev){
        if (ev == null){
            Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
            return;
        }

        Log.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker.");
    }

    /**
     * Create the virtual machines in a datacenter.
     *
     * @param datacenterId Id of the chosen PowerDatacenter
     *
     * @pre $none
     * @post $none
     */
    protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the next one
		int requestedVms = 0;
		String datacenterName = CloudSim.getEntityName(datacenterId);
		for (Vm vm : getVmList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId() + " in " + datacenterName);
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}

		getDatacenterRequestedIdsList().add(datacenterId);

		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

    /**
     * Submit cloudlets to the created VMs.
     *
     * @pre $none
     * @post $none
     */
	protected void submitCloudlets() {
		int vmIndex = 0;
		for (Cloudlet cloudlet : getCloudletList()) {
			Vm vm;
			if (cloudlet.getVmId() == -1) { //if user didn't bind this cloudlet and it has not been executed yet
				vm = getVmsCreatedList().get(vmIndex);
			} else { //submit to the specific vm
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					Log.printLine(CloudSim.clock()+": "+getName()+ ": Postponing execution of cloudlet "+cloudlet.getCloudletId()+": bount VM not available");
					continue;
				}
			}

			Log.printLine(CloudSim.clock()+": "+getName()+ ": Sending cloudlet "+cloudlet.getCloudletId()+" to VM #"+vm.getId());
			cloudlet.setVmId(vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			getCloudletSubmittedList().add(cloudlet);
		}

		// remove submitted cloudlets from waiting list
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
	}

	/**
	 * Destroy the virtual machines running in datacenters.
	 *
	 * @pre $none
	 * @post $none
	 */
	protected void clearDatacenters() {
		for (Vm vm : getVmsCreatedList()) {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Destroying VM #" + vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
		}

		getVmsCreatedList().clear();
	}

	/**
	 * Send an internal event communicating the end of the simulation.
	 *
	 * @pre $none
	 * @post $none
	 */
	private void finishExecution() {
		sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
	}

	/* (non-Javadoc)
	 * @see cloudsim.core.SimEntity#shutdownEntity()
	 */
	@Override
	public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
	}

	/* (non-Javadoc)
	 * @see cloudsim.core.SimEntity#startEntity()
	 */
	@Override
	public void startEntity() {
		Log.printLine(getName() + " is starting...");
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
	}

	/**
	 * Gets the vm list.
	 *
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmList() {
		return (List<T>) vmList;
	}

	/**
	 * Sets the vm list.
	 *
	 * @param <T> the generic type
	 * @param vmList the new vm list
	 */
	protected <T extends Vm> void setVmList(List<T> vmList) {
		this.vmList = vmList;
	}


	/**
	 * Gets the cloudlet list.
	 *
	 * @param <T> the generic type
	 * @return the cloudlet list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletList() {
		return (List<T>) cloudletList;
	}


	/**
	 * Sets the cloudlet list.
	 *
	 * @param <T> the generic type
	 * @param cloudletList the new cloudlet list
	 */
	protected <T extends Cloudlet> void setCloudletList(List<T> cloudletList) {
		this.cloudletList = cloudletList;
	}

	/**
	 * Gets the cloudlet submitted list.
	 *
	 * @param <T> the generic type
	 * @return the cloudlet submitted list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletSubmittedList() {
		return (List<T>) cloudletSubmittedList;
	}


	/**
	 * Sets the cloudlet submitted list.
	 *
	 * @param <T> the generic type
	 * @param cloudletSubmittedList the new cloudlet submitted list
	 */
	protected <T extends Cloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
		this.cloudletSubmittedList = cloudletSubmittedList;
	}

	/**
	 * Gets the cloudlet received list.
	 *
	 * @param <T> the generic type
	 * @return the cloudlet received list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletReceivedList() {
		return (List<T>) cloudletReceivedList;
	}

	/**
	 * Sets the cloudlet received list.
	 *
	 * @param <T> the generic type
	 * @param cloudletReceivedList the new cloudlet received list
	 */
	protected <T extends Cloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
		this.cloudletReceivedList = cloudletReceivedList;
	}

	/**
	 * Gets the vm list.
	 *
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmsCreatedList() {
		return (List<T>) vmsCreatedList;
	}

	/**
	 * Sets the vm list.
	 *
	 * @param <T> the generic type
	 * @param vmsCreatedList the vms created list
	 */
	protected <T extends Vm> void setVmsCreatedList(List<T> vmsCreatedList) {
		this.vmsCreatedList = vmsCreatedList;
	}

	/**
	 * Gets the vms requested.
	 *
	 * @return the vms requested
	 */
	protected int getVmsRequested() {
		return vmsRequested;
	}

	/**
	 * Sets the vms requested.
	 *
	 * @param vmsRequested the new vms requested
	 */
	protected void setVmsRequested(int vmsRequested) {
		this.vmsRequested = vmsRequested;
	}

	/**
	 * Gets the vms acks.
	 *
	 * @return the vms acks
	 */
	protected int getVmsAcks() {
		return vmsAcks;
	}

	/**
	 * Sets the vms acks.
	 *
	 * @param vmsAcks the new vms acks
	 */
	protected void setVmsAcks(int vmsAcks) {
		this.vmsAcks = vmsAcks;
	}

	/**
	 * Increment vms acks.
	 */
	protected void incrementVmsAcks() {
		this.vmsAcks++;
	}

	/**
	 * Gets the vms destroyed.
	 *
	 * @return the vms destroyed
	 */
	protected int getVmsDestroyed() {
		return vmsDestroyed;
	}

	/**
	 * Sets the vms destroyed.
	 *
	 * @param vmsDestroyed the new vms destroyed
	 */
	protected void setVmsDestroyed(int vmsDestroyed) {
		this.vmsDestroyed = vmsDestroyed;
	}

	/**
	 * Gets the datacenter ids list.
	 *
	 * @return the datacenter ids list
	 */
	protected List<Integer> getDatacenterIdsList() {
		return datacenterIdsList;
	}

	/**
	 * Sets the datacenter ids list.
	 *
	 * @param datacenterIdsList the new datacenter ids list
	 */
	protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
		this.datacenterIdsList = datacenterIdsList;
	}

	/**
	 * Gets the vms to datacenters map.
	 *
	 * @return the vms to datacenters map
	 */
	protected Map<Integer, Integer> getVmsToDatacentersMap() {
		return vmsToDatacentersMap;
	}

	/**
	 * Sets the vms to datacenters map.
	 *
	 * @param vmsToDatacentersMap the vms to datacenters map
	 */
	protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
		this.vmsToDatacentersMap = vmsToDatacentersMap;
	}

	/**
	 * Gets the datacenter characteristics list.
	 *
	 * @return the datacenter characteristics list
	 */
	protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
		return datacenterCharacteristicsList;
	}

	/**
	 * Sets the datacenter characteristics list.
	 *
	 * @param datacenterCharacteristicsList the datacenter characteristics list
	 */
	protected void setDatacenterCharacteristicsList(Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
		this.datacenterCharacteristicsList = datacenterCharacteristicsList;
	}

	/**
	 * Gets the datacenter requested ids list.
	 *
	 * @return the datacenter requested ids list
	 */
	protected List<Integer> getDatacenterRequestedIdsList() {
		return datacenterRequestedIdsList;
	}

	/**
	 * Sets the datacenter requested ids list.
	 *
	 * @param datacenterRequestedIdsList the new datacenter requested ids list
	 */
	protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
		this.datacenterRequestedIdsList = datacenterRequestedIdsList;
	}
	
	public void bindCloudletsToVmsSimple()
	{
		double transCost=0.015;
		double transferRate=133;
		int vmNum=vmList.size();
		int cloudletNum=cloudletList.size();
		int idx=0;
		for(int i=0;i<cloudletNum;i++)
		{
			cloudletList.get(i).setVmId(vmList.get(idx).getId());
			idx=(idx+1)%vmNum;
		}
	

                double[] vmclock=new double[cloudletNum];
				double[] clock=new double[cloudletList.size()];
				
				if(cloudletList.size()>0)
					clock=ClockTime(cloudletList,vmclock,clock,transferRate);
				for(int vm=0;vm<vmList.size();vm++)
					for(int i=0;i<cloudletList.size();i++)
						if (vmList.get(vm).getId()==cloudletList.get(i).getVmId()&&vmclock[vm]<clock[i])
							vmclock[vm]=clock[i];
		
					double totalcost=ECost(cloudletList)+TransCost(cloudletList,transCost);
					double totaltime=0;
					for(int j=0;j<vmNum;j++)
					{
						
						if(vmclock[j]>totaltime)
							totaltime=vmclock[j];
					}
					
					
						
					System.out.println("the totaltime is:"+totaltime);
					System.out.println("the ECost is:"+ECost(cloudletList));
					System.out.println("the TransCost is:"+TransCost(cloudletList,transCost));
					System.out.println("the total cost is:"+(ECost(cloudletList)+TransCost(cloudletList,transCost)));
				}
	public void bindPDTsToVmbyRACO()
	{
		int vmNum=vmList.size();
		int cloudletNum=cloudletList.size();
		
		double transCost=0.015;
		double transferRate=100;
		
		
		List<Cloudlet> Workflow;
		Workflow=new ArrayList<Cloudlet> ();
	
		int[] deepth;//任务的深度,后面为工作流中各个任务的深度
		deepth=new int[cloudletList.size()];

		for(int i=0;i<cloudletList.size();i++)
			deepth[i]=cloudletList.get(i).getRequiredFiles().size();

		for(int i=0;i<deepth.length;i++)//Sort deepth 从小到大
			for(int j=i+1;j<deepth.length;j++)
			{
				int temp=0;
				if(deepth[j]<deepth[j-1])
				{
					temp=deepth[j];
					deepth[j]=deepth[j-1];
					deepth[j-1]=temp;

				}
			}
		for(int i=0;i<cloudletList.size();i++)
			for(int j=0;j<cloudletList.size();j++)
				if(deepth[i]==cloudletList.get(j).getRequiredFiles().size()&&!Workflow.contains(cloudletList.get(j)))
					Workflow.add(cloudletList.get(j));

		setCloudletList(Workflow);	
		for(int i=0;i<cloudletList.size();i++)
			System.out.print(cloudletList.get(i).getCloudletId()+"  ");
		System.out.println();
		int antNum=50;
		int Itera=50;//迭代次数
		double[][] p=new double[cloudletNum][vmNum];
		double[][][]sp=new double[antNum][cloudletNum][vmNum];
		double[][][]pheromone=new double[antNum][cloudletNum][vmNum];
		double[][][]Heuristic=new double[antNum][cloudletNum][vmNum];
		double a=0.7;
		double b=0.7;
		double PP=0.3;
		double dis1=(double)1/(double)vmNum;
		for(int m=0;m<antNum;m++)
			for(int i=0;i<cloudletNum;i++)
				for(int j=0;j<vmNum;j++)
				{
					pheromone[m][i][j]=vmList.get(j).getMips()/(double)1000;
					Heuristic[m][i][j]=vmList.get(j).getMips()/(double)1000;
				}
		for(int i=0;i<cloudletNum;i++)
			for(int j=0;j<vmNum;j++)
				p[i][j]=1/(double)vmNum;
		for(int m=0;m<antNum;m++)
			for(int i=0;i<cloudletNum;i++)
				for(int j=0;j<vmNum;j++)
					sp[m][i][j]=p[i][j];
		int[][] bestSolu=new int[antNum][cloudletNum];
		int[][] antSolu=new int[antNum][cloudletNum];
		int flag=0;
		double[][] antClock=new double[antNum][cloudletNum];
		double[]anttime=new double[antNum];
		double Max=99999;
		for(int m=0;m<antNum;m++)
			anttime[m]=Max;
		while(flag<Itera)
		{
			for(int m=0;m<antNum;m++)
			{
				for(int i=0;i<cloudletNum;i++)
				{
					double tempp=0;
					for(int j=0;j<vmNum;j++)
						tempp+=Math.pow(pheromone[m][i][j],a)*Math.pow(Heuristic[m][i][j],b);
					for(int j=0;j<vmNum;j++)
						sp[m][i][j]=Math.pow(pheromone[m][i][j],a)*Math.pow(Heuristic[m][i][j],b)/tempp;
				}
				double minus=0;
				
				for(int i=0;i<cloudletNum;i++)
				{	
					minus=Math.random();
					for(int j=0;j<=vmNum-1;j++)
						{
							minus=minus-sp[m][i][j];
							if(minus<0)
							{
								antSolu[m][i]=j;
								break;
							}
						}
				}
				/*double maxp=0;
				for(int i=0;i<cloudletNum;i++)
				{
					for(int j=0;j<vmNum;j++)
						if(sp[m][i][j]>maxp)
							maxp=sp[m][i][j];
					int[] temp=new int[vmNum];
					int count=0;
					for(int j=0;j<vmNum;j++)
						if(sp[m][i][j]==maxp)
						{
							temp[count]=j;
							count++;//统计等概率的个数
						}
					if(count>1)
					{
						Random r=new Random();
						antSolu[m][i]=temp[r.nextInt(count)];	//将返回一个大于等于0小于cou的随机数
					}
					else antSolu[m][i]=temp[0];
				}*/
				for(int i=0;i<cloudletNum;i++)
				{//System.out.println("######"+i);
					cloudletList.get(i).setVmId(antSolu[m][i]);
				}
				
				double[] vmclock=new double[cloudletNum];
				double[] clock=new double[cloudletList.size()];
				
				if(cloudletList.size()>0)
					clock=ClockTime(cloudletList,vmclock,clock,transferRate);
				for(int vm=0;vm<vmList.size();vm++)
					for(int i=0;i<cloudletList.size();i++)
						if (vmList.get(vm).getId()==cloudletList.get(i).getVmId()&&vmclock[vm]<clock[i])
							vmclock[vm]=clock[i];
				
					for(int i=0;i<cloudletList.size();i++)
						pheromone[m][i][antSolu[m][i]]=(1-PP)*pheromone[m][i][antSolu[m][i]]+dis1;
				
					
					
					
					double totaltime=0;
					for(int j=0;j<vmNum;j++)
					{
						
						if(vmclock[j]>totaltime)
							totaltime=vmclock[j];
					}
					
					
						if(totaltime<anttime[m])
						{
							for(int i=0;i<cloudletNum;i++)
							{
								antClock[m][i]=clock[i];
								bestSolu[m][i]=antSolu[m][i];
							
							}
							anttime[m]=totaltime;
							System.out.println("antclock["+m+"]"+anttime[m]);
						}
					
				
			}
			flag++;
		}



		for(int m=0;m<antNum;m++)
		{
		
			for(int i=0;i<cloudletNum;i++)
				{
				System.out.println("第"+m+"只蚂蚁"+"cloudlet "+cloudletList.get(i).getCloudletId()+" antClock   "+antClock[m][i]+"  bestvm "+bestSolu[m][i]);
		
				}
		
		}
		double mintime=Max;
		for(int i=0;i<antNum;i++)
		{
			if(anttime[i]<mintime)
			{mintime=anttime[i];
				System.out.println("选择哪些蚂蚁"+i);
			}
		}
		
				
		
		
		for(int i=0;i<antNum;i++)
			if(anttime[i]==mintime)
			{
				for(int z=0;z<cloudletNum;z++)
					System.out.println("bestsolution"+bestSolu[i][z]+" cloudlets"+cloudletList.get(z).getCloudletId());
				System.out.println("@@@@@@@@@@@@m="+i);
				for(int j=0;j<cloudletNum;j++)
					cloudletList.get(j).setVmId(vmList.get(bestSolu[i][j]).getId());
			}
		System.out.println("the totaltime is:"+mintime);
		System.out.println("the ECost is:"+ECost(cloudletList));
		System.out.println("the TransCost is:"+TransCost(cloudletList,transCost));
		System.out.println("the total cost is:"+(ECost(cloudletList)+TransCost(cloudletList,transCost)));
		
	}


	public void bindPDTsToVmbyLBRACO()
	{
		int vmNum=vmList.size();
		int cloudletNum=cloudletList.size();
		
		double transCost=0.015;
		double transferRate=100;
		
		
		List<Cloudlet> Workflow;
		Workflow=new ArrayList<Cloudlet> ();
	
		int[] deepth;//任务的深度,后面为工作流中各个任务的深度
		deepth=new int[cloudletList.size()];
		
		int[] width;
		width=new int[cloudletList.size()];
		for(int i=0;i<cloudletList.size();i++)
		{
				for(int j=0;j<cloudletList.size();j++)
				{
					width[0]=0;
					 if(cloudletList.get(i).getRequiredFiles().contains(Integer.toString(cloudletList.get(j).getCloudletId())))
						width[i]=width[j]+1;
				}
			System.out.println("&&&&&&&&&&&width"+width[i]);
		}
		
		for(int i=0;i<cloudletList.size();i++)
			deepth[i]=cloudletList.get(i).getRequiredFiles().size();

		for(int i=0;i<deepth.length;i++)//Sort deepth 从小到大
			for(int j=i+1;j<deepth.length;j++)
			{
				int temp=0;
				if(deepth[j]<deepth[j-1])
				{
					temp=deepth[j];
					deepth[j]=deepth[j-1];
					deepth[j-1]=temp;

				}
			}
		for(int i=0;i<cloudletList.size();i++)
			for(int j=0;j<cloudletList.size();j++)
				if(deepth[i]==cloudletList.get(j).getRequiredFiles().size()&&!Workflow.contains(cloudletList.get(j)))
					Workflow.add(cloudletList.get(j));

		setCloudletList(Workflow);	
		//	System.out.println("&&&&&&&&&&&"+cloudletList.size());
		//Schedule the PDTs by ACO.
		for(int i=0;i<cloudletList.size();i++)
			System.out.print(cloudletList.get(i).getCloudletId()+"  ");
		System.out.println();
		int antNum=50;
		int Itera=50;//迭代次数
		double[][] p=new double[cloudletNum][vmNum];
		double[][][]sp=new double[antNum][cloudletNum][vmNum];
		double[][]pheromone=new double[cloudletNum][vmNum];
		double[][]Heuristic=new double[cloudletNum][vmNum];
		double a=0.7;
		double b=0.7;
		double PP=0.3;
		double add=1/vmNum;
		for(int m=0;m<antNum;m++)
			for(int i=0;i<cloudletNum;i++)
				for(int j=0;j<vmNum;j++)
				{
				pheromone[i][j]=vmList.get(j).getMips()/(double)1000;
				Heuristic[i][j]=vmList.get(j).getMips()/(double)1000;
				}
		for(int i=0;i<cloudletNum;i++)
			for(int j=0;j<vmNum;j++)
				p[i][j]=1/(double)vmNum;
		for(int m=0;m<antNum;m++)
			for(int i=0;i<cloudletNum;i++)
				for(int j=0;j<vmNum;j++)
					sp[m][i][j]=p[i][j];
		int[][] bestSolu=new int[antNum][cloudletNum];
		int[][] antSolu=new int[antNum][cloudletNum];
		int flag=0;
		double[][] antClock=new double[antNum][cloudletNum];
		double[]anttime=new double[antNum];
		double Max=99999;
		
		for(int m=0;m<antNum;m++)
			anttime[m]=Max;
		while(flag<Itera)
		{
			for(int m=0;m<antNum;m++)
			{
				for(int i=0;i<cloudletNum;i++)
				{
					double tempp=0;
					for(int j=0;j<vmNum;j++)
						tempp+=Math.pow(pheromone[i][j],a)*Math.pow(Heuristic[i][j],b);
					for(int j=0;j<vmNum;j++)
						sp[m][i][j]=Math.pow(pheromone[i][j],a)*Math.pow(Heuristic[i][j],b)/tempp;
				}
				//进行轮盘赌
				double minus=0;
				
				for(int i=0;i<cloudletNum;i++)
				{	
					minus=Math.random();
					for(int j=0;j<=vmNum-1;j++)
						{
							minus=minus-sp[m][i][j];
							if(minus<0)
							{
								antSolu[m][i]=j;
								break;
							}
						}
				}
				/*double maxp=0;
				for(int i=0;i<cloudletNum;i++)
				{
					for(int j=0;j<vmNum;j++)
						if(sp[m][i][j]>maxp)
							maxp=sp[m][i][j];
					int[] temp=new int[vmNum];
					int count=0;
					for(int j=0;j<vmNum;j++)
						if(sp[m][i][j]==maxp)
						{
							temp[count]=j;
							count++;//统计等概率的个数
						}
					if(count>1)
					{
						Random r=new Random();
						antSolu[m][i]=temp[r.nextInt((count)%(count-0+1) +0)];	//将返回一个大于等于0小于cou的随机数
					}
					else antSolu[m][i]=temp[0];
				}*/
			
				for(int i=0;i<cloudletNum;i++)
				{//System.out.println("######"+i);
					cloudletList.get(i).setVmId(antSolu[m][i]);
				}
				for(int i=0;i<cloudletNum;i++)
				{
					for(int j=i-1;j>=0;j--)
					{
						if(width[j]==width[i])
						{
							if(cloudletList.get(j).getVmId()==cloudletList.get(i).getVmId())
							{
								
							//进行轮盘赌
							double temp=0;
							temp=Math.random();
								for(int c=0;c<=vmNum-1;c++)
									{
									temp=temp-sp[m][i][c];
										if(temp<0)
										{
											antSolu[m][i]=c;
											break;
										}
									}
							}
								/*Random ran=new Random();
								int f =ran.nextInt(vmNum)%((vmNum)-0+1) +0;
								cloudletList.get(i).setVmId(f);*/
								/*double sum=0;
								for(int z=0;z<vmNum;z++)
									
										
										sum+=Math.pow(pheromone[j][z],a)*Math.pow(Heuristic[j][z],b);
									
								for(int z=0;z<vmNum;z++)
									
										sp[m][j][z]=Math.pow(pheromone[j][z],a)*Math.pow(Heuristic[j][z],b)/sum;
									
							}
							//进行轮盘赌
							double minuss=0;
							
							for(int c=0;c<cloudletNum;c++)
							{	
								minuss=Math.random();
								for(int d=0;d<=vmNum-1;d++)
									{
										minuss=minuss-sp[m][c][d];
										if(minuss<0)
										{
											antSolu[m][c]=d;
											break;
										}
									}
								cloudletList.get(c).setVmId(antSolu[m][c]);
							}*/
							
						}
					}
				}
				for(int i=0;i<cloudletNum;i++)
				{//System.out.println("######"+i);
					cloudletList.get(i).setVmId(antSolu[m][i]);
				}
				/*int[][] cloudnum=new int[5][vmNum];
				int[] aaa=new int[5];
				for(int i=0;i<5;i++)
				{
					
						for(int j=0;j<cloudletNum;j++)
						{
							for(int z=0;z<vmNum;z++)
							{
								if(width[j]==i&&cloudletList.get(j).getVmId()==vmList.get(z).getId())
								{
									cloudnum[i][z]++;
									System.out.println("######cloudnum["+i+"]["+z+"]"+cloudnum[i][z]);
								}	
							}
						}
						
						for(int z=0;z<vmNum;z++)
						{
							aaa[i]+=cloudnum[i][z];
						}	
						System.out.println("aaa["+i+"]"+aaa[i]);
						for(int z=0;z<vmNum;z++)
						{
							if(cloudnum[i][z]> Math.round(aaa[i]/2)); 
							{
								for(int j=0;j<cloudletNum;j++)
								{
									if(width[j]==i&&cloudletList.get(j).getVmId()==vmList.get(z).getId())
									{System.out.println("######");
										Random ran=new Random();
										int x=0;
										while(x<=cloudnum[i][z]/2)
										{
											int f =ran.nextInt(vmNum)%((vmNum)-0+1) +0;
											cloudletList.get(j).setVmId(f);
											x++;
										}
									}
								}
							}
						}
							
				}*/
				
				double[] vmclock=new double[cloudletNum];
				double[] clock=new double[cloudletList.size()];
				
				if(cloudletList.size()>0)
					clock=ClockTime(cloudletList,vmclock,clock,transferRate);
				for(int vm=0;vm<vmList.size();vm++)
					for(int i=0;i<cloudletList.size();i++)
						if (vmList.get(vm).getId()==cloudletList.get(i).getVmId()&&vmclock[vm]<clock[i])
							vmclock[vm]=clock[i];
				
				double []length=new double[vmNum];
				double []percent=new double[vmNum];
				double []PC=new double[vmNum];
				double vmtotal=0;
				
				for(int vm1=0;vm1<vmList.size();vm1++)
				{
					for(int i=0;i<cloudletList.size();i++)
					{	if (vmList.get(vm1).getId()==cloudletList.get(i).getVmId())
						{
							length[vm1]+=cloudletList.get(i).getCloudletLength();	
						}
					}
				}
				for(int vm=0;vm<vmList.size();vm++)
				{
					vmtotal+=length[vm]/vmList.get(vm).getMips();
	
				}
				double avg=vmtotal/4;
				System.out.println("######aaa"+vmtotal);
				for(int vm=0;vm<vmList.size();vm++)
				{
					percent[vm]=length[vm]/vmList.get(vm).getMips();
					PC[vm]=1-(percent[vm]-avg)/vmtotal;
					System.out.println("######aaa"+avg);
					System.out.println("######bbbpercent["+vm+"]"+percent[vm]);
					System.out.println("######bbbPC["+vm+"]"+PC[vm]);
				}
				
				/*
				int []task=new int[vmNum];
				
				for(int vm1=0;vm1<vmList.size();vm1++)
				{
					for(int i=0;i<cloudletList.size();i++)
					{	if (vmList.get(vm1).getId()==cloudletList.get(i).getVmId())
						{
							task[vm1]++;
							
						}
					
					}vmtotal+=60000*task[vm1]/vmList.get(vm1).getMips();
				}*/
					//System.out.println("######aaa"+vmtotal);
					/*
					double totalpercent=0;
					for(int vm=0;vm<vmList.size();vm++)
					{
						if(task[vm]!=0)
						{
							percent[vm]=vmtotal/(60000*task[vm]/vmList.get(vm).getMips());
						}
						else
						{
							percent[vm]=1;
						}
					totalpercent+=percent[vm];
					System.out.println("######bbbpercent["+vm+"]"+percent[vm]);
					}
					System.out.println("totalpercent"+totalpercent);
					for(int vm=0;vm<vmList.size();vm++)
					{
						System.out.println("xxx"+percent[vm]/totalpercent);
					}*/
					
						
						//System.out.println("######bbbtask["+vm+"]"+task[vm]);
						
						//System.out.println("######bbbwp["+vm+"]"+(60000*task[vm]/vmList.get(vm).getMips())/vmtotal);
						//System.out.println("######bbbpercent["+vm+"]"+percent[vm]);
					
					for(int i=0;i<cloudletList.size();i++)
					{
						for(int j=0;j<vmNum;j++)
							{
							
								if(j==antSolu[m][i])
								{
									pheromone[i][antSolu[m][i]]=((1-PP)*pheromone[i][antSolu[m][i]]+1/clock[i])*PC[j];
										
								}
								else
								{
									pheromone[i][j]=pheromone[i][j]*PC[j];
								}
						}
					}	
					double totaltime=0;
					for(int j=0;j<vmNum;j++)
					{
						
						if(vmclock[j]>totaltime)
							totaltime=vmclock[j];
					}
					
						
						double totalcost=ECost(cloudletList)+TransCost(cloudletList,transCost);
						
						/*if(totalcost<antCost[m])
						{*/
						if(totaltime<anttime[m])
						{
							for(int i=0;i<cloudletNum;i++)
							{
								antClock[m][i]=clock[i];
								bestSolu[m][i]=antSolu[m][i];
							
							}
							anttime[m]=totaltime;
							System.out.println("antclock["+m+"]"+anttime[m]);
						}
					
			}
			flag++;
		}



		for(int m=0;m<antNum;m++)
		{
		
			for(int i=0;i<cloudletNum;i++)
				{
				System.out.println("第"+m+"只蚂蚁"+"cloudlet "+cloudletList.get(i).getCloudletId()+" antClock   "+antClock[m][i]+"  bestvm "+bestSolu[m][i]);
		
				}
		
		}
		double mintime=Max;
		for(int i=0;i<antNum;i++)
		{
			if(anttime[i]<mintime)
			{mintime=anttime[i];
				System.out.println("选择哪些蚂蚁"+i);
			}
		}
				
		
		
		for(int i=0;i<antNum;i++)
			if(anttime[i]==mintime)
			{
				for(int z=0;z<cloudletNum;z++)
					System.out.println("bestsolution"+bestSolu[i][z]+" cloudlets"+cloudletList.get(z).getCloudletId());
				System.out.println("@@@@@@@@@@@@m="+i);
				for(int j=0;j<cloudletNum;j++)
					cloudletList.get(j).setVmId(vmList.get(bestSolu[i][j]).getId());
			}
		System.out.println("the totaltime is:"+mintime);
		System.out.println("the ECost is:"+ECost(cloudletList));
		System.out.println("the TransCost is:"+TransCost(cloudletList,transCost));
		System.out.println("the total cost is:"+(ECost(cloudletList)+TransCost(cloudletList,transCost)));
	}

	


	//subswarm clocktime

	public double[] ClockTime(List<? extends Cloudlet> swarm,double[] vmclock,double[] clock,double MaxTransferRate)
	{
			int numT=0;

			while(numT<swarm.size())
			{
				for(int j=0;j<vmList.size();j++)
					if(swarm.get(0).getVmId()==vmList.get(j).getId())
						{
							//vmclock[j]=swarm.get(numT).getCloudletLength()/vmList.get(j).getMips();
							clock[0]=swarm.get(0).getCloudletLength()/vmList.get(j).getMips();
						}
				for(int k=numT;k>=0;k--)
				{
					for(int j=0;j<vmList.size();j++)
					{	
						if(vmList.get(j).getId()==swarm.get(numT).getVmId())
						{
							double mip=vmList.get(j).getMips();
							if(swarm.get(numT).getRequiredFiles().contains(Integer.toString(swarm.get(k).getCloudletId()))&&(clock[numT]-swarm.get(numT).getCloudletLength()/mip)<=clock[k])
								
							{
								
								 if(swarm.get(numT).getRequiredFiles().contains(Integer.toString(swarm.get(k).getCloudletId()))&&vmclock[j]<=clock[k])
								{ 
									clock[numT]=clock[k]+swarm.get(numT).getCloudletLength()/mip;
								}
								else if(swarm.get(numT).getRequiredFiles().contains(Integer.toString(swarm.get(k).getCloudletId()))&&vmclock[j]>clock[k]&&vmclock[j]!=swarm.get(numT).getCloudletLength()/mip)
								{
									clock[numT]=vmclock[j]+swarm.get(numT).getCloudletLength()/mip;
								}
								else if(swarm.get(numT).getRequiredFiles().contains(Integer.toString(swarm.get(k).getCloudletId()))&&vmclock[j]>clock[k]&&vmclock[j]==swarm.get(numT).getCloudletLength()/mip)
								{
									clock[numT]=clock[k]+swarm.get(numT).getCloudletLength()/mip;
								}
							}
								
							
						}
					}
				}
				for(int k=numT;k>=0;k--)
					if(swarm.get(numT).getRequiredFiles().contains(Integer.toString(swarm.get(k).getCloudletId()))&&swarm.get(numT).getVmId()!=swarm.get(k).getVmId())
						clock[numT]+=swarm.get(k).getCloudletOutputSize()/MaxTransferRate;
				
				for(int j=0;j<vmList.size();j++)
					if(swarm.get(numT).getVmId()==vmList.get(j).getId())
						vmclock[j]=clock[numT];

				numT++;

			}
		
		return clock;
	}

	
	public double TransCost(List<? extends Cloudlet> Exelist,double transCost)
	{
		long filesize=0;

		for(int i=0;i<Exelist.size();i++)
		{

			if(Exelist.get(i).requiresFiles())
			{
				List<Integer> list;
				list=new ArrayList<Integer>();
				for(int j=0;j<Exelist.get(i).getRequiredFiles().size();j++)
				{
					list.add(Integer.valueOf((Exelist.get(i).getRequiredFiles().get(j))));
				}
				for(int k=0;k<list.size();k++)
				{
					for(int n=0;n<Exelist.size();n++)
					{
						if(Exelist.get(n).getCloudletId()==list.get(k))
							filesize+=Exelist.get(n).getCloudletOutputSize();
					}
				}

			}
		}
		return filesize*transCost;


	}
	public double ECost(List<? extends Cloudlet> Exelist)//任务的执行cost
	{
		double ECost=0.0;
		for(int i=0;i<Exelist.size();i++)
		{
			for(int j=0;j<vmList.size();j++)
			{
				if(vmList.get(j).getId()==Exelist.get(i).getVmId())
				{
					double mip=vmList.get(j).getMips();
					ECost+=Exelist.get(i).getCloudletLength()/mip;	
				}
			}

		}
		return ECost;
	}
}
