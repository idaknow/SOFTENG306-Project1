package scheduler;

import java.util.ArrayList;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import scheduler.Greedy.QueueItem;
import scheduler.Schedule;

import org.graphstream.graph.Edge;
/**
 * This class provides methods that the Schedule data structure can use
 *
 */
public class ScheduleHelper {

	/**
	 * Finds all the root nodes of the input graph
	 * @param g
	 * @returns all the root nodes
	 */
	public static ArrayList<Integer> findRootNodes(Graph g) {
		ArrayList<Integer> rootNodes = new ArrayList<Integer>();
		int i = 0;

		for (Node n:g) {
			if (n.getInDegree() == 0) {
				rootNodes.add(i);
			}
			i++;
		}
		return rootNodes;
	}

	/**
	 * Get's the weight of the input node, and returns its weight
	 * @param g
	 * @param nodeIndex
	 * @return
	 */
	public static int getNodeWeight(Graph g, int nodeIndex){
		return (int)Double.parseDouble(g.getNode(nodeIndex).getAttribute("Weight").toString());
	}


	/**
	 * After a node has been processed, this method is used to return all new nodes that can be processed
	 * @param g
	 * @param nodeIndex
	 * @return
	 */
	public static ArrayList<Integer> processableNodes(Graph g, int nodeIndex) {

		ArrayList<Integer> processableNodes = new ArrayList<Integer>();
		boolean nodeProcessable;
		
		Iterable<Edge> ite = g.getNode(nodeIndex).getEachLeavingEdge(); // Gets all the leaving edges of the node just processed

		// Gets all the child nodes of the node that was just processed, for each child node, check whether all it's parent nodes have been processed
		for (Edge e: ite) {
			Node n = e.getNode1();
			nodeProcessable = true;

			// Check all of child node's parent nodes have been processed
			Iterable<Edge> childIte = g.getNode(n.getId()).getEachEnteringEdge();
			for (Edge childEdge: childIte) {
				Node parentNode = childEdge.getNode0();

				if ((int)Double.parseDouble(parentNode.getAttribute("processorID").toString()) == -1) {
					nodeProcessable = false;
					break;
				}
			}

			if (nodeProcessable == true) {
				processableNodes.add(n.getIndex());
			}
		}
		return processableNodes;
	}

	/**
	 * Returns the cost of putting the queue item into the processor
	 * @param schedule
	 * @param q
	 * @param g
	 * @return
	 */
	public static int[] scheduleNode(Schedule schedule, QueueItem q, Graph g) {

		int minimumProcLength;
		int procWaitTime = 0;
		int nodeWeight = getNodeWeight(g, q.nodeIndex);
		
		ArrayList<Integer> parentNodeCosts = new ArrayList<Integer>(); // This stores the cost of putting the queue item into the specified pid when coming from each parent node
		ArrayList<Node> parentNodes = new ArrayList<Node>(); // Stores the parent node queue item comes from
		

		if (g.getNode(q.nodeIndex).getInDegree() != 0) {
			//Get the post-processed processorLength of the queueitem from each of the parent nodes
			for (Edge e:g.getNode(q.nodeIndex).getEachEnteringEdge()) {
				Node parentNode = e.getNode0();
				int parentProcessor = (int)Double.parseDouble(parentNode.getAttribute("processorID").toString());

				//if parent node was processed on the same processor than the queue item can be added with just nodeWeight
				if (q.processorID == parentProcessor) {
					parentNodeCosts.add(schedule.procLengths[q.processorID] + nodeWeight);
					parentNodes.add(parentNode);
				} else {
					//parent node was not processed on the same processor

					//need to find when the parent node finished processing
					int parentNodeFinishedProcessing = (int)Double.parseDouble(parentNode.getAttribute("Start").toString()) + getNodeWeight(g, parentNode.getIndex());

					//if the parent node finished processing longer than the weight of the edge to the child then can add automatically to the processor
					if (schedule.procLengths[q.processorID] - parentNodeFinishedProcessing >= (int)Double.parseDouble(e.getAttribute("Weight").toString())){
						parentNodeCosts.add(schedule.procLengths[q.processorID] + nodeWeight);
						parentNodes.add(parentNode);
					} else {
						//find out how long need to wait before can add to processor

						//time left to wait
						int timeToWait = (int)Double.parseDouble(e.getAttribute("Weight").toString()) - (schedule.procLengths[q.processorID] - parentNodeFinishedProcessing);

						if (timeToWait < 0) {
							timeToWait = 0;
						}
						parentNodeCosts.add(schedule.procLengths[q.processorID] + nodeWeight + timeToWait);
						parentNodes.add(parentNode);
					}
				}

			}

			minimumProcLength = parentNodeCosts.get(0);
			
			int temp = 0;
			for(int i = 0; i < parentNodeCosts.size() - 1; i++) {
				int pNodeCost = parentNodeCosts.get(i);
				if (pNodeCost < minimumProcLength) {
					minimumProcLength = pNodeCost;
					temp = i;
				}
			}
			
			Node p = parentNodes.get(temp);
			
			if ((int)Double.parseDouble(p.getAttribute("processorID").toString()) != q.processorID) {
				procWaitTime = minimumProcLength - nodeWeight - schedule.procLengths[q.processorID];
			}
			
		} else {
			minimumProcLength = getNodeWeight(g, q.nodeIndex) + schedule.procLengths[q.processorID];
		}
		
		int[] newProcLengthAndTimeToWait = {minimumProcLength, procWaitTime};
		return newProcLengthAndTimeToWait;

	}
}
