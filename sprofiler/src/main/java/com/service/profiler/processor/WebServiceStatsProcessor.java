package com.service.profiler.processor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.service.profiler.dto.WebServiceStatsDTO;
import com.service.profiler.store.DbStore;


public class WebServiceStatsProcessor {
	private static Logger log = Logger.getLogger(WebServiceStatsProcessor.class);
	public static synchronized void processStats(String key,List<String> reqRes, String status, Date entry, Date exit,Map<Integer,Map<String, Long>> serviceMap) {
		WebServiceStatsDTO stats=new WebServiceStatsDTO();
		stats.setKey(key);
		stats.setStatus(status);
		stats.setEntry(entry);
		stats.setExit(exit);
		stats.setRequest(reqRes.get(0));
		stats.setResponse(reqRes.get(1));
		storeContent(stats,serviceMap);
		//storeChildContent(serviceMap,parentId);
		printStats(stats);
		printMap(serviceMap);
	}
	

	private static synchronized void storeContent(final WebServiceStatsDTO stats,final Map<Integer,Map<String, Long>> serviceMap) {
		System.out.println("calling the store content here");
		// TODO Auto-generated method stub
            try {
				ExecutorService executorService = Executors.newFixedThreadPool(10);
				
				executorService.execute(new Runnable() {
					long autoGeneratedID=0;
					Connection dbConnection = null;
					PreparedStatement preparedStatement = null;
					PreparedStatement preparedStatement1 = null;
					ResultSet rs = null;
					String query = "Insert into web_service_utility"
							+ "(module,service_name,request_xml,response_xml,total_elapsed_time,call_status) VALUES"
							+ "(?,?,?,?,?,?)";
					
					String[] serviceDetails = stats.getKey().split("-");
					String moduleName = serviceDetails[1].substring(0,serviceDetails[1].length()-8).trim();
					String serviceName = serviceDetails[3].trim();
					
				    public void run() {
				    	try {
							dbConnection = getDBConnection();
							preparedStatement = dbConnection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
							preparedStatement.setString(1, moduleName);
							preparedStatement.setString(2, serviceName);
							preparedStatement.setString(3, stats.getRequest());
							preparedStatement.setString(4, stats.getResponse());
							preparedStatement.setLong(5, (stats.getExit().getTime() - stats.getEntry().getTime()));
							preparedStatement.setString(6, stats.getStatus());
							preparedStatement.executeUpdate();
							rs = preparedStatement.getGeneratedKeys();
							
							while(rs.next()){
								autoGeneratedID = rs.getInt(1);
								if(serviceMap != null){
									System.out.println("contents......................................."+serviceMap.toString());
									Iterator<Entry<Integer, Map<String, Long>>> mapIt = serviceMap.entrySet().iterator();
									 while (mapIt.hasNext()) {
										 Entry<Integer, Map<String, Long>> mapEntry = mapIt.next();
									    	System.out.println("chiledMap......................................"+mapEntry.toString());
									    	Iterator<Entry<String,Long>> childIt = mapEntry.getValue().entrySet().iterator();
									    	while (childIt.hasNext()) {
									    		 Entry<String,Long> childService = childIt.next();
										    	 System.out.println("childservice......................................."+childService.toString());
										    	 String query = "Insert into child_service"
														+ "(moduleName,time,utility_id) VALUES"
														+ "(?,?,?)";
										    	    preparedStatement1 = dbConnection.prepareStatement(query);
													preparedStatement1.setString(1, (String) childService.getKey());
													preparedStatement1.setLong(2, (long) childService.getValue());
													preparedStatement1.setLong(3, autoGeneratedID);
													preparedStatement1.executeUpdate();
									    	}
									 }
								}
							}
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}finally {

							if (preparedStatement != null) {
								try {
									preparedStatement.close();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							if (preparedStatement1 != null) {
								try {
									preparedStatement.close();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							if (rs != null) {
								try {
									rs.close();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							if (dbConnection != null) {
								try {
									dbConnection.close();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						}
				    }
				});
				executorService.shutdown();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
	
	private static synchronized void storeChildContent(Map<Integer,Map<String, Long>> serviceMap,
			Long parentId) {
		System.out.println("calling the child content here");
		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		if(serviceMap != null){
			System.out.println("contents......................................."+serviceMap.toString());
			Iterator<Entry<Integer, Map<String, Long>>> mapIt = serviceMap.entrySet().iterator();
		    while (mapIt.hasNext()) {
		    	Entry<Integer, Map<String, Long>> mapEntry = mapIt.next();
		    	System.out.println("chiledMap......................................"+mapEntry.toString());
		    	Iterator<Entry<String,Long>> childIt = mapEntry.getValue().entrySet().iterator();
		    	 while (childIt.hasNext()) {
		    		 Entry<String,Long> childService = childIt.next();
		    	 System.out.println("childservice......................................."+childService.toString());
		    	
		    	String query = "Insert into child_service"
						+ "(moduleName,time,utility_id) VALUES"
						+ "(?,?,?)";
		    	try {
					dbConnection = getDBConnection();
					preparedStatement = dbConnection.prepareStatement(query);
					preparedStatement.setString(1, (String) childService.getKey());
					preparedStatement.setLong(2, (long) childService.getValue());
					preparedStatement.setLong(3, parentId);
					preparedStatement.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally {

					if (preparedStatement != null) {
						try {
							preparedStatement.close();
							//ServiceTracker.serviceMap = new HashMap<String, Long>();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (dbConnection != null) {
						try {
							dbConnection.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
		    }
		}
	}
}


	private static synchronized Connection getDBConnection() {
		Connection dbConnection = null;
		try {
			Class.forName(DbStore.getDriver());

		} catch (ClassNotFoundException e) {
			//log
			System.out.println(e.getMessage());
		}
		try {
			dbConnection = DriverManager.getConnection(
                            DbStore.getUrl(), DbStore.getUsername(),DbStore.getPassword());
			return dbConnection;

		} catch (SQLException e) {
			//log
			System.out.println(e.getMessage());
		}
		return dbConnection;

	}


	private static synchronized void printStats(WebServiceStatsDTO stats) {
		System.out.println("printing the printStats here");
		log.info("webservice: ["+stats.getKey()+"], request : ["+stats.getRequest()+"], response : ["+stats.getResponse()+"], status: "+stats.getStatus()+(stats.getStatus().equals("SUCCESS")?", time: "+(stats.getExit().getTime()-stats.getEntry().getTime())+"ms":""));
	}
	
		public static synchronized void printMap(Map map) {
			if(map != null){
		    Iterator it = map.entrySet().iterator();
		    while (it.hasNext()) {
		    	Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		       // it.remove(); // avoids a ConcurrentModificationException
		    }
			}
		}


}
