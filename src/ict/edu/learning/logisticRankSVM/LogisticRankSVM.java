package ict.edu.learning.logisticRankSVM;

import ict.edu.learning.measure.Measurement;
import ict.edu.learning.multiThread.ThreadCalculateObj_Jfun;
import ict.edu.learning.multiThread.ThreadUpdateVMatrix;
import ict.edu.learning.utilities.FileUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.features.Normalizer;
import ciir.umass.edu.features.SumNormalizor;
import ciir.umass.edu.features.ZScoreNormalizor;
import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Matrix;
import ciir.umass.edu.learning.PartialPair;
import ciir.umass.edu.learning.PartialPairList;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.Vector;
import ciir.umass.edu.metric.ERRScorer;

public class LogisticRankSVM extends Ranker{

	/**
	 * @param args
	 */
	public static boolean letor = false;
	public static boolean mustHaveRelDoc = false;
	public static boolean normalize = false;
	public static Normalizer nml = new SumNormalizor();
	public static int partialPairTotalNum = 100;
	public static int RowsOfVMatrix = 100;
	public static int ColsOfVMatrix = 5;
	public static int ROW_INCREASE = 20;
	public static int V_size = 0;
	public static double epsilon = 0.00000000001f;
	public static int nThread = 1;
	public static double learningRate = 0.005;
	public static double maxIterations = 1000;
	public static int learningRateAttenuationTime = 5;
	public static int NDCG_para = 10;
	public static HashMap<String, Integer> hp_V = null;
	public static void main(String[] args) throws InterruptedException, Exception {
		// TODO Auto-generated method stub
		String[] rType = new String[]{"MART", "RankNet", "RankBoost", "AdaRank", "Coordinate Ascent", "LambdaRank", "LambdaMART", "ListNet", "Random Forests","Logistic RanKSVM"};
		RANKER_TYPE[] rType2 = new RANKER_TYPE[]{RANKER_TYPE.MART, RANKER_TYPE.RANKNET, RANKER_TYPE.RANKBOOST, RANKER_TYPE.ADARANK, RANKER_TYPE.COOR_ASCENT, RANKER_TYPE.LAMBDARANK, RANKER_TYPE.LAMBDAMART, RANKER_TYPE.LISTNET, RANKER_TYPE.RANDOM_FOREST,RANKER_TYPE.LOGISTIC_RANKSVM};
		
		String trainFile = "";
		String featureDescriptionFile = "";
		double ttSplit = 0.0;//train-test split
		double tvSplit = 0.0;//train-validation split
		int foldCV = -1;
		String validationFile = "";
		String testFile = "";
		int rankerType = 10;//our own logistic ranksvm
		String trainMetric = "ERR@10";
		String testMetric = "";
		
		String savedModelFile = "";
		String rankFile = "";
		boolean printIndividual = false;
		
		//for my personal use
		String indriRankingFile = "";
		String scoreFile = "";		
		if(args.length < 2)
		{
			
			System.out.println("not enough parameter");
			return;
		}
		
		for(int i=0;i<args.length;i++)
		{
			if(args[i].compareTo("-train")==0)
				trainFile = args[++i];
			else if(args[i].compareTo("-ranker")==0)
				rankerType = Integer.parseInt(args[++i]);
			else if(args[i].compareTo("-feature")==0)
				featureDescriptionFile = args[++i];
			else if(args[i].compareTo("-metric2t")==0)
				trainMetric = args[++i];
			else if(args[i].compareTo("-metric2T")==0)
				testMetric = args[++i];
			else if(args[i].compareTo("-nThread")==0)
				nThread = Integer.parseInt(args[++i]);
			else if(args[i].compareTo("-learningRate")==0)
				learningRate = Double.parseDouble(args[++i]);
			else if(args[i].compareTo("-maxIterations")==0)
				maxIterations = Double.parseDouble(args[++i]);
			else if(args[i].compareTo("-epsilon")==0)
				epsilon = Double.parseDouble(args[++i]);
			else if(args[i].compareTo("-gmax")==0)
				ERRScorer.MAX = Math.pow(2, Double.parseDouble(args[++i]));						
			else if(args[i].compareTo("-tts")==0)
				ttSplit = Double.parseDouble(args[++i]);
			else if(args[i].compareTo("-tvs")==0)
				tvSplit = Double.parseDouble(args[++i]);
			else if(args[i].compareTo("-kcv")==0)
				foldCV = Integer.parseInt(args[++i]);
			else if(args[i].compareTo("-validate")==0)
				validationFile = args[++i];
			else if(args[i].compareTo("-test")==0)
				testFile = args[++i];
			else if(args[i].compareTo("-norm")==0)
			{
				
				String n = args[++i];
				if(n.compareTo("sum") == 0)
					{
						nml = new SumNormalizor();
						normalize = true;
					}
				else if(n.compareTo("zscore") == 0)
					{
						nml = new ZScoreNormalizor();
						normalize = true;	
					}
				else
				{
					System.out.println("Unknown normalizor: " + n);
					System.out.println("System will now exit.");
					System.exit(1);
				}
			}		
			else
			{
				System.out.println("Unknown command-line parameter: " + args[i]);                                                                                                       
				System.out.println("System will now exit.");
				System.exit(1);
			}
		}
				
		LogisticRankSVM logi_rankSvm=new LogisticRankSVM();
		long startTime=System.currentTimeMillis();   
		System.out.println("program starts");
		logi_rankSvm.evaluate(trainFile, validationFile, testFile, "");
		long endTime=System.currentTimeMillis(); 
		System.out.println("past time:"+(endTime-startTime)/1000+"s");

	}
	
	public List<RankList> readInput(String inputFile)	
	{
		FeatureManager fm = new FeatureManager();
		List<RankList> samples = fm.read3(inputFile);//read3(String fn) is defined myself for sake of my own experiment
		return samples;
	}
	public int[] readFeature(String featureDefFile)
	{
		FeatureManager fm = new FeatureManager();
		int[] features = fm.getFeatureIDFromFile(featureDefFile);
		return features;
	}
	public void normalize(List<RankList> samples, int[] fids)
	{
		for(int i=0;i<samples.size();i++)
			nml.normalize(samples.get(i), fids);
	}
	public int[] getFeatureFromSampleVector(List<RankList> samples)
	{
		DataPoint dp = samples.get(0).get(0);
		int fc = dp.getFeatureCount();
		int[] features = new int[fc];
		for(int i=0;i<fc;i++)
			features[i] = i+1;
		return features;
	}
	public List<PartialPairList> getPartialPairForAllQueries(List<RankList> rll)
	{
		List<PartialPairList> ppll =new ArrayList<PartialPairList>();
		//int num=0;
		for (int i = 0; i < rll.size(); i++) {
			PartialPairList tem = getPartialPairForOneQuery(rll.get(i));
			ppll.add(tem);
			//num++;
		}
		//System.out.println(num);
		return ppll;
	}
	// convert labeled documents from one query into partialPair for the same query 
	public PartialPairList getPartialPairForOneQuery(RankList rl)//rl holds all documents for one query 
	{
		PartialPairList ppl = new PartialPairList();
		for (int i = 0; i < rl.size(); i++) {
			for (int j = i+1; j < rl.size(); j++) {
				if(rl.get(i).getLabel()!=(rl.get(j).getLabel())){
					ppl.add(new PartialPair(rl.get(i),rl.get(j)));
				}
			}
		}
		return ppl;
		
	}
	public List<String> getAllPartialPairID(List<PartialPairList> ppll)
	{
		List<String> strl = new ArrayList<String>();
		for (int i = 0; i < ppll.size(); i++) {
			for (int j = 0; j < ppll.get(i).size(); j++) {
				strl.add(ppll.get(i).get(j).getPartialPairID());
			}
		}
		return strl;
	}
		
	public List<List<String>> getVRowsID(List<RankList> rll)
	{
		List<List<String>> sll = new ArrayList<List<String>>();
		for (int i = 0; i < rll.size(); i++) {
			List<String> sl = new ArrayList<String>();
			for (int j = 0; j < rll.get(i).size(); j++) {
				//put ith query's relative document id into a list
				sl.add(rll.get(i).get(j).getDocID());
			}
			sll.add(sl);
		}
		return sll;
	}
	public int RowSize_V(List<RankList> rll){
		int total = 0;
		
		for (int i = 0; i < rll.size(); i++) {			
			total += rll.get(i).size();
		}
		return total;
	}
	public HashMap<String, Integer>  getRowIDofVMatrix(List<RankList> rll){
		
		HashMap<String, Integer> hp = new HashMap<String, Integer>();
		int index = 0;
		for (int i = 0; i < rll.size(); i++) {
			for (int j = 0; j < rll.get(i).size(); j++) {				
				String key=rll.get(i).get(j).getID() + "-" + rll.get(i).get(j).getDocID();
				
				hp.put(key, index);
				index++;
			}
		}
		return hp;
		
	}
	public Matrix updateVMatrix(Matrix V_pre, List<PartialPairList> ppll, List<RankList> rll){
		HashMap<String, Integer> hp = getRowIDofVMatrix(rll);
		// get the derivative of V_ac 
		for (int i = 0; i < rll.size(); i++) {
			for (int j = 0; j < rll.get(i).size(); j++) {
				//iterate every vector V_ac
				
			}
		}
		//
		return null;
	}
	public Matrix parallel_sgd_random_JFun(PartialPair pp, Matrix V_old, List<PartialPairList> ppll, List<RankList> rll, int nThread) throws InterruptedException{
		HashMap<String, Integer> hp = hp_V;
//		Matrix V_new = new Matrix(V_old); 
		//double eta = Math.pow(10, -3);
		Matrix V_new = new Matrix(V_old);
		//we use V_iq and V_jq to stand for the row id of the corresponding documents related to partialPair pp
		int V_iq = hp.get(pp.getQueryID() + "-" + pp.getLargeDocID()).intValue();
		int V_jq = hp.get(pp.getQueryID() + "-" + pp.getSmallDocID()).intValue();
		double index_E = 0;
		double factor1 = 0f;
		for (int k = 0; k < ppll.size(); k++) {
			for (int l = 0; l < ppll.get(k).size(); l++) {
				//for a given partialPair X_ijq=ppll.get(i).get(j), we need to compute the 
				double innerProduct_V = V_old.getInnerProduct(V_iq, V_jq);				
				double innerProduct_partialPair = pp.dotProduct(ppll.get(k).get(l));
				index_E += innerProduct_V * innerProduct_partialPair; 
			}
		} 
		if(index_E>20)
			factor1 = 0;
		else if(index_E<-20)
			factor1 = 1;
		else
			factor1 = 1/(1+Math.exp(index_E));
		if(factor1==0){
			System.out.println("the gradient is 0 for partialPair " + pp.getPartialPairID());
			return null;
		}
		// we parallelize the calculation
		ExecutorService es = Executors.newFixedThreadPool(nThread);
		List<Future<double[]>> resultList = new ArrayList<Future<double[]>>();
		//next we calculate the gradients of matrix V,ie all the elements in matrix V
		
		for (int i = 0; i < rll.size(); i++) {//iterate all the queries
			for (int j = 0; j < rll.get(i).size(); j++) {// iterate all the documents of query i
				// find out the partialPairs which dataPoint=rll.get(i).get(j) involves, 
				int V_ac = hp_V.get(rll.get(i).get(j).getID() + "-" + rll.get(i).get(j).getDocID());				
				Future<double[]> fu = es.submit(new ThreadUpdateVMatrix(factor1, i, pp, V_ac, ppll, hp, V_old, learningRate));	
				resultList.add(fu);
						
			}//end of iterating documents under the same query
		}//end of iterating queries
		
		es.shutdown();		 
		while (!es.awaitTermination(10, TimeUnit.SECONDS));		
		for (int i = 0; i < resultList.size(); i++) {
			try {
				V_new.setRowVector(resultList.get(i).get(), i);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return V_new;
	}
	public Matrix sgd_random_JFun(PartialPair pp, Matrix V_old,  List<PartialPairList> ppll, List<RankList> rll){
		HashMap<String, Integer> hp = hp_V;
		Matrix V_new = new Matrix(V_old);
		double eta = Math.pow(10, -3);
		//we use V_iq and V_jq to stand for the row id of the corresponding documents related to partialPair pp
		int V_iq = hp.get(pp.getQueryID() + "-" + pp.getLargeDocID()).intValue();
		int V_jq = hp.get(pp.getQueryID() + "-" + pp.getSmallDocID()).intValue();
		double index_E = 0;
		double factor1 = 0f;
		for (int k = 0; k < ppll.size(); k++) {
			for (int l = 0; l < ppll.get(k).size(); l++) {
				//for a given partialPair X_ijq=ppll.get(i).get(j), we need to compute the 
				double innerProduct_V = V_old.getInnerProduct(V_iq, V_jq);				
				double innerProduct_partialPair = pp.dotProduct(ppll.get(k).get(l));
				index_E += innerProduct_V * innerProduct_partialPair;
			}
		}
		if(index_E>20)
			factor1 = 0;
		else if(index_E<-20)
			factor1 = 1;
		else
			factor1 = 1/(1+Math.exp(index_E));
					
		//next we calculate the gradients of matrix V,ie all the elements in matrix V
		for (int i = 0; i < rll.size(); i++) {//iterate all the queries
			for (int j = 0; j < rll.get(i).size(); j++) {// iterate all the documents of query i
				// find out the partialPairs which dataPoint=rll.get(i).get(j) involves, 
				double [] factor2 = new double[Matrix.ColsOfVMatrix];	
				int V_ac = hp_V.get(rll.get(i).get(j).getID() + "-" + rll.get(i).get(j).getDocID());
				for (int j2 = 0; j2 < ppll.get(i).size(); j2++) {
					double [] temp = new double[Matrix.ColsOfVMatrix];
					
					PartialPair ite_pp = ppll.get(i).get(j2);
					String qid_largeDoc = ite_pp.getQueryID() + "-" + ite_pp.getLargeDocID();
					String qid_smallDoc = ite_pp.getQueryID() + "-" + ite_pp.getSmallDocID();
					if (V_ac == hp.get(qid_largeDoc)) {
						int docID_associatedWithV_ac = hp.get(qid_smallDoc);
						double multiplier =pp.dotProduct(ite_pp);
						// parameter factor2, stores the result of multiplication
						V_old.multiplyRowVector(docID_associatedWithV_ac, multiplier, temp);
						Matrix.RowVectorAddition(factor2, temp);
						//flag = true;
					}
					else if(V_ac == hp.get(qid_smallDoc)){
						int docID_associatedWithV_ac = hp.get(qid_largeDoc);
						double multiplier = pp.dotProduct(ite_pp);
						V_old.multiplyRowVector(docID_associatedWithV_ac, multiplier, temp);
						Matrix.RowVectorAddition(factor2, temp);
						//flag = true;
					}
				}
				double[] gradient = Matrix.multiplyRowVector(-factor1, factor2);
				Matrix.RowVectorAddition(V_new.getV()[V_ac], Matrix.multiplyRowVector(eta, gradient));
			}//end of iterating documents under the same query
		}//end of iterating queries
		return V_new;
		
	}
	public double[] derivative_JFun(String V_rowID, Matrix V_old,  List<PartialPairList> ppll, List<RankList> rll){
		HashMap<String, Integer> hp = hp_V;
		//the next two for loop to iterate every partialPair		
		double[] der_value = new double[Matrix.ColsOfVMatrix];
		double[] total_der_value = new double[Matrix.ColsOfVMatrix];
		for (int i = 0; i < ppll.size(); i++) {			
			for (int j = 0; j < ppll.get(i).size(); j++) {				
				//every single partialPair, we need to compute factor1 and factor2, get the result of factor1*factor2
				double factor1 = 0f;
				/*double factor1_numerator = 0;
				double factor1_denominator =0;*/
				double index_E = 0;
				double [] derivative = new double[Matrix.ColsOfVMatrix];
				for (int kk = 0; kk < ppll.size(); kk++) {
					for (int ll = 0; ll < ppll.get(kk).size(); ll++) {
						//for a given partialPair X_ijq=ppll.get(i).get(j), we need to compute the  
						
						String queryID = ppll.get(kk).get(ll).getQueryID();
						String largeDocID = ppll.get(kk).get(ll).getLargeDocID();
						String smallDocID = ppll.get(kk).get(ll).getSmallDocID();
						int V_iq = hp.get(queryID+"-"+largeDocID).intValue();
						int V_jq = hp.get(queryID+"-"+smallDocID).intValue();
						double innerProduct_V = V_old.getInnerProduct(V_iq, V_jq);
						double innerProduct_partialPair = ppll.get(i).get(j).dotProduct(ppll.get(kk).get(ll));
						index_E += innerProduct_V * innerProduct_partialPair;
					}
				}
				/*factor1_numerator = Math.exp(-index_E);
				factor1_denominator = 1+Math.exp(-index_E);*/
				//factor1 = Math.exp(-index_E)/(1+Math.exp(-index_E));
				if(index_E>20)
					factor1 = 0;
				else if(index_E<-20)
					factor1 = 1;
				else
					factor1 = 1/(1+Math.exp(index_E));
				double [] factor2 = new double[Matrix.ColsOfVMatrix];				
				String s = V_rowID.substring(0, V_rowID.indexOf("-")).trim();
				for (int k = 0; k < ppll.size(); k++) {					
					//we only focus on the query which the derivated document vector related to
					//ppll.get(k).size()>0 , which ensures there're at least one partialPair under a query
					//ppll.get(k).get(0).getQueryID().equals(s), which ensures we have found the query which the
					//derivated document belongs to
					if(ppll.get(k).size()>0 && ppll.get(k).get(0).getQueryID().equals(s)){
						// when we find the query derivated document belonging to, we find another document related
						//to the derivated document under the same query						
						for (int l = 0; l < ppll.get(k).size(); l++) {
							//boolean flag = false;
							double [] temp = new double[Matrix.ColsOfVMatrix];
							int V_ac = hp_V.get(V_rowID);
							PartialPair curr_pp = ppll.get(k).get(l);
							String qid_largeDoc = curr_pp.getQueryID() + "-" + curr_pp.getLargeDocID();
							String qid_smallDoc = curr_pp.getQueryID() + "-" + curr_pp.getSmallDocID();
							if (V_ac == hp.get(qid_largeDoc)) {
								int docID_associatedWithV_ac = hp.get(qid_smallDoc);
								double multiplier = ppll.get(i).get(j).dotProduct(curr_pp);
								// parameter factor2, stores the result of multiplication
								V_old.multiplyRowVector(docID_associatedWithV_ac, multiplier, temp);
								Matrix.RowVectorAddition(factor2, temp);
								//flag = true;
							}
							else if(V_ac == hp.get(qid_smallDoc)){
								int docID_associatedWithV_ac = hp.get(qid_largeDoc);
								double multiplier = ppll.get(i).get(j).dotProduct(curr_pp);
								V_old.multiplyRowVector(docID_associatedWithV_ac, multiplier, temp);
								Matrix.RowVectorAddition(factor2, temp);
								//flag = true;
							}
							/*if(flag == true){
								
								Matrix.SetRowVector(der_value, Matrix.multiplyRowVector(-factor1, factor2));
								//	der_value = Matrix.multiplyRowVector(-factor1, factor2);
								Matrix.RowVectorAddition(total_der_value, der_value);
							}*/
							
						}
						break;
					}	
					
					
				}
				Matrix.SetRowVector(der_value, Matrix.multiplyRowVector(-factor1, factor2));
				//	der_value = Matrix.multiplyRowVector(-factor1, factor2);
				Matrix.RowVectorAddition(total_der_value, der_value);
				
			}
			System.out.println(i);
		}
		return total_der_value;
	}
	public double calculateObj_Jfun ( List<PartialPairList> ppll, Matrix V){
		HashMap<String, Integer> hp = hp_V;
		//the next two for loop to iterate every partialPair
		double J_value = 0f;
		for (int i = 0; i < ppll.size(); i++) {
			for (int j = 0; j < ppll.get(i).size(); j++) {				
				//every single partialPair, we need to compute ln(....)
				double index_E = 0f;
				for (int k = 0; k < ppll.size(); k++) {
					for (int l = 0; l < ppll.get(k).size(); l++) {
						//for a given partialPair X_ijq=ppll.get(i).get(j), we need to compute the  
						/*if(ppll.get(k).size()==0)
							continue;*/
						String queryID = ppll.get(k).get(l).getQueryID();
						String largeDocID = ppll.get(k).get(l).getLargeDocID();
						String smallDocID = ppll.get(k).get(l).getSmallDocID();
						int V_iq = hp.get(queryID+"-"+largeDocID).intValue();
						int V_jq = hp.get(queryID+"-"+smallDocID).intValue();
						double innerProduct_V = V.getInnerProduct(V_iq, V_jq);
						double innerProduct_partialPair = ppll.get(i).get(j).dotProduct(ppll.get(k).get(l));
						index_E += innerProduct_V * innerProduct_partialPair;
					}
				}
				if(index_E>10)
					J_value += 0;
				else if(index_E<-10)
					J_value += (-index_E);
				else					
					J_value += Math.log(1+Math.exp(-index_E));
			}
			System.out.println(i);
		}
		return J_value;
	}
	public double parallelCalculateObj_Jfun ( List<PartialPairList> ppll, Matrix V, int nThread) throws InterruptedException, Exception{
		HashMap<String, Integer> hp = hp_V;
		//the next two for loop to iterate every partialPair
		double J_value = 0f;
		ExecutorService es = Executors.newFixedThreadPool(nThread);
		List<Future<Double>> resultList = new ArrayList<Future<Double>>();
		for (int i = 0; i < ppll.size(); i++) {
			Future<Double> fu = es.submit(new ThreadCalculateObj_Jfun( ppll, i, V, hp));			
			resultList.add(fu);
		}
		es.shutdown();
		while (!es.awaitTermination(10, TimeUnit.SECONDS));
		for (Future<Double> future : resultList) {
			J_value += future.get().doubleValue();
		}
		
		return J_value;
	}
	public Boolean isConverge(double[][] V1, double[][] V2, int rows, int cols, double epsilon){
		double error=0f;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				error += Math.abs(V1[i][j]-V2[i][j]);
			}
		}
		if (error <= epsilon) {
			return true;
		} else {
			return false;
		}		
	}
	public PartialPair  getPP_RandomQuery(List<PartialPairList> ppll){
		Random rand = new Random();
		 //
		boolean flag = true;
		int query_index = 0;
		int pp_query_index = 0;
		while(flag){
			query_index = rand.nextInt(ppll.size());
			int pp_num = ppll.get(query_index).size();
			if(pp_num!=0){
				flag = false;
				pp_query_index = rand.nextInt(pp_num);
			}
		}
		return ppll.get(query_index).get(pp_query_index);		
		
	}
	public List<ArrayList<Double>> getScoreByFun(List<RankList> rll,Matrix matrixV){
		List<ArrayList<Double>> dll = new ArrayList<ArrayList<Double>>();
//		List<PartialPairList> ppll = getPartialPairForAllQueries(rll);		
		Vector w = getW(rll, matrixV);
		for (int i = 0; i < rll.size(); i++) {
			ArrayList<Double> dl = new ArrayList<Double>();
			for (int j = 0; j < rll.get(i).size(); j++) {
				Vector x_ij = new Vector(rll.get(i).get(j).getFeatureVector());
				double scoreByFun = Vector.dotProduct(w, x_ij);
				dl.add(scoreByFun);
			}
			dll.add(dl);
		}
		return dll;		
	}
	public Vector getW(List<RankList> rll, Matrix matrixV){
		List<PartialPairList> ppll = getPartialPairForAllQueries(rll);		
		Vector w = new Vector(Matrix.getColsOfVMatrix());
		for (int i = 0; i < ppll.size(); i++) {
			for (int j = 0; j < ppll.get(i).size(); j++) {
				PartialPair pp = ppll.get(i).get(j);
				String qid = pp.getQueryID();
				String largeDocID = qid + pp.getLargeDocID();
				String smallDocID = qid + pp.getSmallDocID();
				int v_iq = hp_V.get(largeDocID);
				int v_jq = hp_V.get(smallDocID);
				double factor = matrixV.getInnerProduct(v_iq, v_jq);
				double [] temp = Matrix.multiplyRowVector(factor, pp.getPartialFVals());
				w = Vector.addition(w, new Vector(temp));
			}
		}
		return w;
	}
	public void evaluate(String trainFile, String validationFile, String testFile, String featureDefFile) throws InterruptedException, Exception
	{
		List<RankList> rll_train = readInput(trainFile);//read input
		hp_V = getRowIDofVMatrix(rll_train);
		List<RankList> rll_validation = null;
              if(validationFile.compareTo("")!=0)
			rll_validation = readInput(validationFile);
		List<RankList> rll_test = null;
		if(testFile.compareTo("")!=0)
			rll_test = readInput(testFile);
		int[] features = readFeature(featureDefFile);//read features
		if(features == null)//no features specified ==> use all features in the training file
			features = getFeatureFromSampleVector(rll_train);		
		if(normalize)
		{
			normalize(rll_train, features);
			if(rll_validation != null)
				normalize(rll_validation, features);
			if(rll_test != null)
				normalize(rll_test, features);
		}	
		// get all partialPairs sorted by different queries
		Matrix v = learn(rll_train);
		/*Matrix v = new Matrix(); 
    	v.randomize();*/
		SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd-HH-mm");
		String date = sdf.format(new Date());
		String filename = "output/matrixV/matrixV"+date+".txt";
		FileUtils.write2File(filename, v, filename);
		Matrix v2 = FileUtils.readFromFileGetMatrix(filename);
		List<ArrayList<Double>> dll_train = getScoreByFun(rll_train,v2);
		List<ArrayList<Double>> dll_vali = getScoreByFun(rll_validation,v2);
		List<ArrayList<Double>> dll_test = getScoreByFun(rll_test,v2);
		double map1 = Measurement.MAP(dll_train, rll_train);
		double map2 = Measurement.MAP(dll_vali, rll_validation);
		double map3 = Measurement.MAP(dll_test, rll_test);
		StringBuffer sb = new StringBuffer();
		sb.append("MAP").append(System.getProperty("line.separator"));
		sb.append("\t train"+"\t validation" +"\t test").append(System.getProperty("line.separator"));
		sb.append("\t" + map1 + "\t" + map2 +"\t" + map3).append(System.getProperty("line.separator"));
		sb.append("NDCG").append(System.getProperty("line.separator"));
		sb.append("\t train"+"\t validation" +"\t test").append(System.getProperty("line.separator"));
		System.out.println("map for train:vili:test:" + map1 + ":" + map2 +":" + map3);
		for (int i = 0; i < NDCG_para; i++) {
			double ndcg_1 = Measurement.NDCG(dll_train, rll_train,i);
			double ndcg_2 = Measurement.NDCG(dll_vali, rll_validation,i);
			double ndcg_3 = Measurement.NDCG(dll_test, rll_test,i);
			sb.append(i+"\t"+ndcg_1+"\t"+ndcg_2+"\t"+ndcg_3).append(System.getProperty("line.separator"));			
		}
		System.out.println(sb.toString());
		System.out.println("learning process over");
	}
	/**
	 * HAVE TO BE OVER-RIDDEN IN SUB-CLASSES
	 */
	public void init()
	{
		
	}
	public Matrix learn(List<RankList> train) throws InterruptedException, Exception
	{
		List<PartialPairList> ppll = getPartialPairForAllQueries(train);
		List<RankList> rll = train;
		System.out.println(getAllPartialPairID(ppll).size());
		/*List<String> rowID_V = getRowIDofVMatrix(train);*/
		Matrix.RowsOfVMatrix = RowSize_V(train);
		Matrix V_0 = new Matrix();
		V_0.randomize();
//		Matrix V_pre = new Matrix(V_0);
//		Matrix V_new = new Matrix(V_0); 
		Matrix V_temp = new Matrix(V_0); 
		Matrix V = new Matrix(V_0);
		long startTime = 0;
		long endTime = 0;
		double Jfun_pre = Double.MAX_VALUE-1;
		double Jfun_new = Double.MAX_VALUE;		
//		int learningRateAttenuationTime = 5;
		int invalidCount = 0;
		int validCount = 0;
		startTime=System.currentTimeMillis();   //start the time	
		System.out.println(new Date());
		Jfun_new = parallelCalculateObj_Jfun(ppll, V, nThread);
		System.out.println(new Date());
		endTime=System.currentTimeMillis();
		System.out.println("the time of calculating Jfun_pre in minutes: "+(endTime-startTime)/1000+" s");
		PartialPair pp = null;
		do{
	//		V_pre = V_new;	
			Jfun_pre = Jfun_new;
			startTime=System.currentTimeMillis();   //start the time	
			System.out.println(new Date());
			do{
				pp = getPP_RandomQuery(ppll);
				V_temp = parallel_sgd_random_JFun( pp,  V, ppll,  rll, nThread);
				/*if(V_new==null){
			//		System.out.println("invalidNum " + (++invalidCount) + ": gradient isn't changed for partialPair " + pp.getPartialPairID());
				}
				else{
					validCount++;
					System.out.println("validNum " + validCount);					
				}*/
			}while(V_temp==null);	
	        endTime=System.currentTimeMillis(); //end the time
	        System.out.println(new Date());
			System.out.println("the time of updating V with a random PartialPair in minutes: "+(endTime-startTime)/1000+" mins");
			Jfun_new = parallelCalculateObj_Jfun(ppll, V_temp, nThread);
	//		Jfun_new = Double.MAX_VALUE;			
			if(Jfun_new<Jfun_pre){
				V = V_temp;
				validCount++;		
				String description = "current learningRate is:" + learningRate + ",after " + validCount + "rounds , the V_new Matrix is:";
				if (validCount%3==0) {
					FileUtils.write2File("MatrixV.txt", V, description);
					System.out.println("Jfun_pre = "+Jfun_pre);
					System.out.println("Jfun_new = " + Jfun_new);
					System.out.println("round " + validCount + ", the difference is " + (Jfun_pre-Jfun_new));	
				}		
			}
			else{
				if(learningRateAttenuationTime>0){
					while(Jfun_new>Jfun_pre){
						LogisticRankSVM.learningRate /=2;
						V_temp = parallel_sgd_random_JFun( pp,  V, ppll,  rll, nThread);						
						Jfun_new = parallelCalculateObj_Jfun(ppll, V_temp, nThread);
					}			
					V = V_temp;
					learningRateAttenuationTime--;
					validCount++;
					continue;
				}
				
				System.out.println("Jfun_pre = "+Jfun_pre);
				System.out.println("Jfun_new = " + Jfun_new);
				System.out.println("Jfun_new has been larger than Jfun_pre, exit now");
				System.out.println("round " + validCount + ", the difference is " + Math.abs(Jfun_new-Jfun_pre));
				break;
			}
	        
		}while(Jfun_pre-Jfun_new>epsilon && validCount < maxIterations);
		return V;
    }
	
	

}
