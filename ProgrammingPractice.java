/**
 * Denny Ho
 * CSC 460
 * 
 * Program has a menu that can insert and query statements based on user input. 
 * Program is connected to oracle database.
 */
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class ProgrammingPractice {
	private static final String connect_string = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
    private static Connection m_conn;
	
	public static void main(String[] args) throws Exception {
		
		Statement s = null;
		PreparedStatement pstmt = null;
				
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			// Get a connection
			m_conn = DriverManager.getConnection(connect_string,"dennyho","a6361");  
			if (m_conn == null) 
				throw new Exception("getConnection failed");
			// Create a statement, resultset
			s = m_conn.createStatement(); 
			ResultSet rs = null;
			
			boolean cont = true;
			Scanner scan = new Scanner(System.in);
			int option;
			
			while (cont) { 
				// MENU
				System.out.println();
				System.out.println("1) Enter member information");
				System.out.println("2) List the member(s) who have paid more than $400");
				System.out.println("3) Add a problem into a specified problem pool");
				System.out.println("4) List the problems and problem pools that each member has");
				System.out.println("5) List the problems that are built upon");
				System.out.println("6) Quit");

				System.out.println();
				System.out.print("Enter 1-5 or 6 to quit: ");

				option = scan.nextInt();
				scan.nextLine();
				System.out.println();
				
				// Add Member
				if (option == 1) {	
					PreparedStatement pstmtM = m_conn.prepareStatement("INSERT INTO Member VALUES "+" (?, ?, ?, ?, ?, ?, ?) " );
					
					String str;
					
					System.out.print("Enter first name: ");
					str = scan.nextLine();
					pstmtM.setString(1, str);
					
					System.out.print("Enter last name: ");
					str = scan.nextLine();	
					pstmtM.setString(2, str);
					
					System.out.print("Enter email address: ");
					str = scan.nextLine();
					pstmtM.setString(3, str);
					
					System.out.print("Is paying member (T or F): ");
					String val = scan.nextLine();
					if (val.equals("T")) pstmtM.setString(4, "y");
					else if (val.equals("F")) pstmtM.setString(4, "n");
					else continue;
					
					System.out.print("Is staff member (T or F): ");
					val = scan.nextLine();
					if (val.equals("T")) pstmtM.setString(5, "y");
					else if (val.equals("F")) pstmtM.setString(5, "n");
					else continue;
					
					System.out.print("Is problem contributor (T or F): ");
					val = scan.nextLine();
					if (val.equals("T")) pstmtM.setString(6, "y");
					else if (val.equals("F")) pstmtM.setString(6, "n");
					else continue;
					
					System.out.print("Enter subscription start date (as MMDDYY, e.g., 120219): ");
					val = scan.nextLine();
					if (val.length() != 6) continue; 
					try {
						Integer num = new Integer(val);
					} catch (Exception e) {
						continue;
					}
					pstmtM.setDate(7, java.sql.Date.valueOf("20"+val.substring(4, 6) +"-"+val.substring(0, 2)+"-"+val.substring(0, 2)));
					
					try {
						int count = pstmtM.executeUpdate();
						System.out.println("New member added!");
					} catch (Exception e) {
						System.out.println("This member already exists!");
					}
				} 
				// List members who paid $400+
				else if (option == 2) {
					rs = s.executeQuery("SELECT* FROM Member WHERE (MONTHS_BETWEEN(TO_DATE('12/12/2019', "
							+ "'MM/DD/YYYY'),subscriptionStartDate) * 8 > 400) AND (isPayingMember = 'y')");
					boolean flag = true;
					while(rs.next()) {
						flag = false;
						System.out.println(rs.getString(3));
					}
					if (flag) System.out.println("No qualified paying member exists!");
				} 
				// Add problem into specified problem pool
				else if (option == 3) {
					// Check Pools Exist
					rs = s.executeQuery("SELECT* FROM ProblemsPool");
					boolean flag = true;
					if (rs.next()) flag = false;
					if (flag) System.out.println("No problem pools exist!");
					else {
						// Prompt for email
						System.out.print("Enter email address for the member having the relevant problem pool: ");
						String email = scan.nextLine();
						
						// Check if member exists
						rs = s.executeQuery("SELECT* FROM Member WHERE ('" + email + "' = emailAddress)");
						if (rs.next() == false) System.out.println("No member exists with this email!");
						else {
							HashMap<Integer, String> pools = new HashMap<Integer, String>();
							
							// Display all of the member's problems pools
							rs = s.executeQuery("SELECT poolName FROM ProblemsPool WHERE ('" + email + "' = emailAddress)");
							int count = 1;
							while (rs.next()) {
								System.out.println(count + " " + rs.getString(1));
								pools.put(count, rs.getString(1));
								count++;
							}
							
							// Check if member has problem pools
							if (count == 1) System.out.println("This member has no problem pool!");
							else {
								// Prompt for Pool Number
								System.out.print("Enter the number of problem pool desired: ");
								int val = scan.nextInt();
								scan.nextLine();
								
								if (val >= count | val < 1) System.out.println("Invalid selection!");
								else {
									HashMap<Integer, String> problems = new HashMap<Integer, String>();
									
									// Display all problems available
									rs = s.executeQuery("SELECT* FROM Problem");
									int probCount = 1;									
									while (rs.next()) {
										System.out.println(probCount + " " + rs.getString(1));
										problems.put(probCount, rs.getString(1));
										probCount++;
									}
									
									// Check if problems exist
									if (probCount == 1) System.out.println("No problem exists!");
									else {
										// Prompt for problem number
										System.out.println("Enter the number of problem desired: ");
										int probNum = scan.nextInt();
										if (probNum >= probCount | probNum < 1) System.out.println("Invalid selection!");
										else {
											// Set up INSERT
											PreparedStatement pstmtP = m_conn.prepareStatement("INSERT INTO ComposedOf VALUES "+" (?, ?, ?) " );
											String title = problems.get(probNum);
											String poolName = pools.get(val);
											pstmtP.setString(1, title);
											pstmtP.setString(2, email);
											pstmtP.setString(3, poolName);
											
											// Add Problem to Pool
											try {
												pstmtP.executeUpdate();
												System.out.println("Problem " + title + " successfully added to problem pool \"" 
															+ poolName + "\" of member having email address " + email);
											} catch (Exception e) {
												System.out.println("This problem already exists in the pool " + poolName + "!");
											}
										}
									}
								}
							}
						}
					}
					
				} 
				// List the problems and problem pools that each member has
				else if (option == 4) {
					// Calculate Max Lengths
					ResultSet tempRS = null;
					tempRS = s.executeQuery("SELECT MAX(LENGTH(firstName)+LENGTH(lastName)) FROM Member "
							+ "JOIN ComposedOf ON Member.emailAddress = ComposedOf.emailAddress");
					tempRS.next();
					int lenN = Math.max(new Integer(tempRS.getString(1))+1,6);
					
					tempRS = s.executeQuery("SELECT MAX(LENGTH(poolName)) FROM Member "
							+ "JOIN ComposedOf ON Member.emailAddress = ComposedOf.emailAddress");
					tempRS.next();
					int lenPool = Math.max(new Integer(tempRS.getString(1)), 12);
					
					tempRS = s.executeQuery("SELECT MAX(LENGTH(Problem.title)) FROM Problem "
							+ "JOIN ComposedOf ON Problem.title = ComposedOf.title");
					tempRS.next();
					int lenTitle = Math.max(new Integer(tempRS.getString(1)),7);
					
					
					rs = s.executeQuery("SELECT Member.firstName, Member.lastName, ComposedOf.poolName, ComposedOf.title, Problem.difficulty" + 
							" FROM Member JOIN ComposedOf ON Member.emailAddress = ComposedOf.emailAddress" + 
							" JOIN Problem ON ComposedOf.title = Problem.title" + 
							" ORDER BY lastName, poolName, title");
					
					String nameC = "";
					String poolC = "";
					String titleC = "";
					String diffC = "";
					int num = 0;
					
					System.out.printf("%-"+lenN+"s  %-"+lenPool+ "s  %-"+lenTitle+"s  %s", "Member", "Problem Pool", "Problem", "Difficulty");
					System.out.println();
					while (rs.next()) {
						String name = rs.getString(1) + " " + rs.getString(2);
						String pool = rs.getString(3);
						String title = rs.getString(4);
						String diff  = rs.getString(5);
						
						// Omit the duplicate strings
						if (num == 0) {
							num = 1;
							nameC = name;
							poolC = pool;
							titleC = title;
							diffC = diff;
						} else {
							if (nameC.equals(name)) {
								name = "";
								if (poolC.equals(pool)) pool = "";
								else poolC = pool;
							}
							else {
								nameC = name;
								poolC = pool;
							}
							
						}
						
					
						

						System.out.printf("%-"+lenN+"s  %-"+lenPool+ "s  %-"+lenTitle+"s  %s", name, pool, title, diff);
						System.out.println();
					}
					
				} else if (option == 5) {
					rs = s.executeQuery("SELECT * FROM BuildUpon ORDER BY thisProblem");
					boolean flag = true;
					while (rs.next()) {
						flag = false;
						System.out.println(rs.getString(1) + ", " + rs.getString(2));
					}
					if (flag) System.out.println("No Problems in Build Upon");
				}
				else if (option == 6) break;
				else continue;
			}
		
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}



