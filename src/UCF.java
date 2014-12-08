import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.fit.pdfdom.PDFToHTML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class UCF {
	static ArrayList<Heading> headT = new ArrayList<Heading>();
	static Connection conn;

	static void ConnectionSetup() {
		try {
			String userName = "expertnetWeb";
			String password = "Kaliban01.";
			String url = "jdbc:jtds:sqlserver://cimes3.its.fsu.edu/ExpertNet2_dev;instance=SQLEXPRESS";

			Class.forName("net.sourceforge.jtds.jdbc.Driver");
			conn = DriverManager.getConnection(url, userName, password);
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}

	}

	static void CreateList() {
		try {
			Document doc = Jsoup.connect(
					"http://tt.research.ucf.edu/technology-locator/").get();
			Elements divHead = doc.select(".post-type-search-term").select(
					".post-type-search-term > div");// .get(0).select(".tt-search-subheader");//.select(".tt-search-subheader");
			// Elements divHead =
			// doc.select(".post-type-search-term").select("div").select(".tt-search-header");
			Elements Header = divHead.select(".tt-search-header");
			for (int i = 0; i < Header.size(); i++) {
				headT.add(new Heading());
				headT.get(i).Heading = Header.get(i).text();
				System.out.println("Heading " + headT.get(i).Heading);
				Elements subHead = divHead.get(i)
						.select(".tt-search-subheader");
				for (int j = 0; j < subHead.size(); j++) {
					int subHEadingIndex = headT.get(i).subH.size();
					headT.get(i).subH.add(new SubHeading());
					headT.get(i).subH.get(subHEadingIndex).subHeading = subHead
							.get(j).text();
					// Data
					Elements Data = divHead.get(i)
							.select(".row.tt-search-docs").get(j)
							.select(".pdf");
					for (int k = 0; k < Data.size(); k++) {
						String heading = toTitleCase(headT.get(i).Heading);
						String subHeading = toTitleCase(headT.get(i).subH
								.get(subHEadingIndex).subHeading);
						int dataIndex = headT.get(i).subH.get(subHEadingIndex).ref
								.size();
						headT.get(i).subH.get(subHEadingIndex).ref
								.add(new Materials());
						Materials matemp = headT.get(i).subH
								.get(subHEadingIndex).ref.get(dataIndex);
						matemp.heading = toTitleCase(heading);
						matemp.subHeading = toTitleCase(subHeading);
						matemp.keyword = heading.trim() + ", "
								+ subHeading.trim();
						matemp.ID = Data.get(k).attr("data-post-id");
						matemp.Title = Data.get(k).select("a").text();
						matemp.url = Data.get(k).select("a").attr("href");
						// System.out.println(matemp.url);

						// Massaging Description

						// text = DeleteUnWanted(text, matemp.Title);
						// matemp.Description = massageDescription(text);
						// ;ConvertToHtml();
						// DeleteUnWanted(text, matemp.Title);

						// addInventorInfo(matemp);

						boolean foundDuplicate = findMatchID(headT,
								headT.get(i).subH.get(subHEadingIndex).ref
										.get(dataIndex).ID, heading,
								subHeading,
								headT.get(i).subH.get(subHEadingIndex).ref
										.get(dataIndex).Title);
						if (foundDuplicate) {
							headT.get(i).subH.get(subHEadingIndex).ref
									.remove(dataIndex);
						} else {
							// Massaging Description
							String text=ExtractFromPDF(matemp.url);
							matemp.Email = extractEmail(text);
							text = DeleteUnWanted(text, matemp.Title);
							matemp.Description = massageDescription(text);
						}

					}
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static String ExtractFromPDF(String url) {
		
		PDDocument pdf = null;
		PDFTextStripper stripper=null;
		try {
			stripper = new PDFTextStripper();
		} catch (IOException e1) {
			// TODO Auto-generated catch block					
		}
		String text = "";
		while (true) {
			boolean exc = false;
			try {
				pdf = PDDocument.load(new URL(url.replace("https", "http")));
				if (new File("back.pdf").exists()) {
					new File("back.pdf").delete();
				}
				// pdf.save("back.pdf");
				text = stripper.getText(pdf);
				text = text.replaceAll("", "");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				exc = true;
			} finally {
				try {
					pdf.close();
				} catch (Exception e) {
					exc = true;
				}
			}

			if (exc || text.equals("") || text.equals(null)) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		return text;

	}

	static String massageDescription(String Text) {

		Text = Text.replaceAll("\r\n", "<br />");
		Text = Text.replaceAll("\n", "<br />");
		Text = Text.replaceAll("\t", " ");
		// Text = Text.replaceAll("\r", "<br />");
		Text = ReplaceAllTo(Text, "Advantages");
		Text = ReplaceAllTo(Text, "Keywords");
		Text = ReplaceAllTo(Text, "Invention");
		Text = ReplaceAllTo(Text, "Background");
		Text = ReplaceAllTo(Text, "Application");
		Text = ReplaceAllTo(Text, "Applications");
		Text = ReplaceAllTo(Text, "Technical Details");

		Text = ReplaceAllTo(Text, "Tech Fields");
		Text = ReplaceAllTo(Text, "Publications");
		Text = ReplaceAllTo(Text, "US Issued Patents");
		Text = ReplaceAllTo(Text, "Licensing Opportunity");
		Text = ReplaceAllTo(Text, "Benefits");
		Text = ReplaceAllTo(Text, "Benefits ");

		Text = ReplaceAllTo(Text, "Advantages");
		Text = ReplaceAllTo(Text, "Selected References");

		Text = ReplaceAllTo(Text, "Lead Inventor");
		Text = ReplaceAllTo(Text, "UCF Inventor");
		Text = ReplaceAllTo(Text, "UCF Inventors");
		Text = ReplaceAllTo(Text, "Lead Inventors");
		Text = ReplaceAllTo(Text, "Inventors");
		Text = ReplaceAllTo(Text, "Inventor");
		return Text;

	}

	static String DeleteUnWanted(String str, String title) {
		str = ReplaceWithNullEntireLine(str, "University of Central Florida");
		str = ReplaceWithNullEntireLine(str,
				"Technology Available for Licensing");
		str = ReplaceWithNullEntireLine(str, "research.ucf.edu");
		str = ReplaceWithNullEntireLine(str, "Orlando, FL 32826");
		str = ReplaceWithNullEntireLine(str, "tt.research.ucf.edu");
		str = ReplaceWithNullEntireLine(str, title);

		str = ReplaceWithNullEntireLine(str, "12201 Research Parkway");
		str = ReplaceWithNullEntireLine(str, "UCF Office of Technology");
		// str=ReplaceWithNullEntireLine(str, "12201 Research Parkway");
		// str=ReplaceWithNullEntireLine(str, "Phone: (407) 822-1136");
		// str=ReplaceWithNullEntireLine(str, "Phone: (407) 822-1136");

		return str;

	}

	static String ReplaceAllTo(String Text, String ReplaceWord) {
		return Text.replaceAll(ReplaceWord + "<br />", "<br /><strong>"
				+ ReplaceWord + "</strong><br />");
	}

	static String ConvertToHtml() throws IOException {
		String args[] = { "back.pdf", "html" };
		PDFToHTML.main(args);
		return deserializeString(new File("html"));

	}

	public static String deserializeString(File file) throws IOException {
		int len;
		char[] chr = new char[4096];
		final StringBuffer buffer = new StringBuffer();
		final FileReader reader = new FileReader(file);
		try {
			while ((len = reader.read(chr)) > 0) {
				buffer.append(chr, 0, len);
			}
		} finally {
			reader.close();
			new File("back.pdf").delete();
			file.delete();
		}
		return buffer.toString();
	}

	static void addInventorInfo(Materials matemp) {

		String text = matemp.Description;
		String inventorNames = GetInventorNames(text);
		String[] names = inventorNames.split(";");
		for (int i = 0; i < names.length; i++) {
			if (names[i].trim().equals(""))
				continue;
			matemp.inventor.add(new Inventor());
			String[] nameIn = names[i].trim().split("\\s+");
			String fn = "";
			String ln = "";
			if (nameIn.length >= 1) {
				fn = nameIn[0];
				for (int j = 1; j < nameIn.length; j++) {
					ln += nameIn[j] + " ";
				}

			}

			fn = fn.trim();
			ln = ln.trim();
			matemp.inventor.get(matemp.inventor.size() - 1).uid = matemp.ID;
			matemp.inventor.get(matemp.inventor.size() - 1).FirstName = toTitleCase(fn);
			matemp.inventor.get(matemp.inventor.size() - 1).LastName = toTitleCase(ln);
		}

	}

	static String ReplaceWithNullEntireLine(String text, String TextToFind) {
		return text.replaceAll(".*" + TextToFind + ".*(\r?\n|\r)?", "");
	}

	static void DisplayInventors() {
		int cnt = 1;
		System.out
				.println("<tr><th>Number</th><th>URL</th><th>First Name</th><th>Last Name</th><th>UID</th></tr>");
		for (int j = 0; j < headT.size(); j++) {
			// System.out.println("Heading:\t" + headT.get(j).Heading);
			for (int j2 = 0; j2 < headT.get(j).subH.size(); j2++) {
				for (int k = 0; k < headT.get(j).subH.get(j2).ref.size(); k++) {
					Materials m = headT.get(j).subH.get(j2).ref.get(k);
					for (int i = 0; i < m.inventor.size(); i++) {
						System.out.print("<tr>");
						System.out.print("<td>" + (cnt++) + "</td><td>" + m.url
								+ "</td><td>"
								+ m.inventor.get(i).FirstName.trim()
								+ "</td><td>"
								+ m.inventor.get(i).LastName.trim()
								+ "</td><td>" + m.inventor.get(i).uid
								+ "</td></tr>");
						System.out.println();

					}

				}
			}
		}
	}

	static void Diplay() {
		int cnt = 1;
		System.out.println("Output");
		System.out
				.println("<tr><th>Number</th><th>Heading</th><th>subheading</th><th>Keyword</th><th>UID</th><th> Title</th><th> URL</th><th> Email</th><th> Description</th></tr>");
		for (int j = 0; j < headT.size(); j++) {
			// System.out.println("Heading:\t" + headT.get(j).Heading);
			for (int j2 = 0; j2 < headT.get(j).subH.size(); j2++) {
				for (int k = 0; k < headT.get(j).subH.get(j2).ref.size(); k++) {

					System.out.print("<tr>");
					System.out.print("<td>" + (cnt++) + "</td><td>"
							+ headT.get(j).subH.get(j2).ref.get(k).heading
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).subHeading
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).keyword
							+ "</td>");
					System.out
							.print("<td>"
									+ headT.get(j).subH.get(j2).ref.get(k).ID
									+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).Title
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).url
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).Email
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).Description +

							"</td>");
					System.out.print("</tr>");
					System.out.println();
				}
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();

		System.out.println("Creating List");
		CreateList();
		System.out.println("Send Data To DB");
		DBHandle();
		System.out.println("Output");
		Diplay();
		// DisplayInventors();
		long end = System.currentTimeMillis();

		System.out.println((end - start) + " ms");
	}

	static boolean findMatchID(ArrayList<Heading> headT, String ID,
			String HeadingT, String SubHeadingT, String Title) {
		boolean found = false;
		for (int h = 0; h < headT.size(); h++) {
			for (int i = 0; i < headT.get(h).subH.size(); i++) {
				for (int j = 0; j < headT.get(h).subH.get(i).ref.size(); j++) {
					if (headT.get(h).subH.get(i).ref.get(j).ID.equals(ID)) {
						// System.out.println("ID MAtch Found \n at "+headT.get(h).subH.get(i).subHeading+" Which is present in "+SubHeadingT);
						if (!checkForDuplicateKeywords(
								headT.get(h).subH.get(i).ref.get(j).keyword,
								HeadingT, "")) {
							// System.out.println("Heading mismatch from "+headT.get(h).Heading+"At"+HeadingT);
							// headT.get(h).subH.get(i).ref.get(j).heading +=
							// ","
							// + HeadingT;
							headT.get(h).subH.get(i).ref.get(j).keyword += ", "
									+ HeadingT.trim();
							found = true;
						}
						if (SubHeadingT == null)
							SubHeadingT = "";
						if (headT.get(h).subH.get(i).ref.get(j).subHeading == null)
							headT.get(h).subH.get(i).ref.get(j).subHeading = "";
						if (!checkForDuplicateKeywords(
								headT.get(h).subH.get(i).ref.get(j).keyword,
								SubHeadingT, HeadingT)) {
							// System.out.println("SubHeading mismatch from "+headT.get(h).subH.get(i).subHeading+"At"+SubHeadingT);
							if (checkForDuplicateKeywords(
									headT.get(h).subH.get(i).ref.get(j).keyword,
									HeadingT, "")) {
								String[] split = headT.get(h).subH.get(i).ref
										.get(j).keyword.split(",");
								for (int k = 0; k < split.length; k++) {
									if (split[k].trim().equals(HeadingT)) {
										split[k] += ", " + SubHeadingT.trim();
										break;
									}
								}
								String temp = split[0];
								for (int k = 1; k < split.length; k++) {
									temp += ", " + split[k].trim();
								}
								headT.get(h).subH.get(i).ref.get(j).keyword = temp
										.trim();
							}
							// headT.get(h).subH.get(i).ref.get(j).keyword +=
							// keyword;
							found = true;
						}
						if (!headT.get(h).subH.get(i).ref.get(j).Title
								.contains(Title)) {
							// System.out.println("Title Mismatch");
							// headT.get(h).subH.get(i).ref.get(j).Title +=
							// ", "+ Title;
							found = true;
						}
						if (found == true) {
							return found;
						}
					}
				}
			}
		}
		return found;
	}

	static boolean checkForDuplicateKeywords(String keywords, String match,
			String heading) {
		String[] split = null;
		if (heading.equals("")) {
			split = keywords.split(",");
		} else {
			split = (keywords.substring(keywords.indexOf(heading))).split(",");
		}
		for (int k = 0; k < split.length; k++) {
			if (split[k].trim().equals(match.trim())) {
				return true;
			}
		}
		return false;
	}

	static void DBHandle() {
		ConnectionSetup();
		for (int j = 0; j < headT.size(); j++) {
			// System.out.println("Heading:\t" + headT.get(j).Heading);
			for (int j2 = 0; j2 < headT.get(j).subH.size(); j2++) {
				for (int k = 0; k < headT.get(j).subH.get(j2).ref.size(); k++) {
					try {
						String sql = "INSERT INTO dbo.propertyHolding (infoURL, contactEmail, fkUniversityID, Title,Keywords,universituid,MarketingPDF,Description) "
								+ "VALUES (?, ?, ?, ?,?,?,?,?)";
						PreparedStatement statement = conn
								.prepareStatement(sql);
						statement.setString(1,
								headT.get(j).subH.get(j2).ref.get(k).url);
						statement.setString(2,
								headT.get(j).subH.get(j2).ref.get(k).Email);
						statement.setInt(3, 6);
						statement.setString(4,
								headT.get(j).subH.get(j2).ref.get(k).Title);
						String keywords = headT.get(j).subH.get(j2).ref.get(k).keyword;
						statement.setString(5, keywords);
						statement.setString(6,
								headT.get(j).subH.get(j2).ref.get(k).ID);
						statement.setString(7,
								headT.get(j).subH.get(j2).ref.get(k).url);
						statement
								.setString(
										8,
										headT.get(j).subH.get(j2).ref.get(k).Description);
						int rowsInserted = statement.executeUpdate();
						if (rowsInserted > 0) {
							System.out
									.println("A new record was inserted successfully!");
						} else {
							System.out
									.println("A new record was inserted Failed!");
						}
					} catch (Exception e) {
						System.out.println("A new record was inserted Failed!");
					}
				}
			}
		}
	}

	public static String toTitleCase(String givenString) {
		if (givenString.equals(null))
			return "";
		if (givenString.equals(""))
			return "";
		if (givenString.equals(" "))
			return " ";
		givenString = givenString.toLowerCase();
		// System.out.println(givenString);
		String[] arr = givenString.split("\\s+");
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			sb.append(Character.toUpperCase(arr[i].charAt(0)))
					.append(arr[i].substring(1)).append(" ");
		}
		return sb.toString().trim();
	}

	static String extractEmail(String content) {
		String email = null;
		String regex = "(\\w+)(\\.\\w+)*@(\\w+\\.)(\\w+)(\\.\\w+)*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			email = matcher.group();

			if (!isValidEmailAddress(email)) {
				email = null;
			}

			break;
		}
		return email;
	}

	static boolean isValidEmailAddress(String emailAddress) {
		String expression = "^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		CharSequence inputStr = emailAddress;
		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(inputStr);
		return matcher.matches();

	}

	static String GetInventorNames(String text) {
		try {
			CharsetEncoder asciiEncoder = Charset.forName("US-ASCII")
					.newEncoder();
			String inventors = "";
			text = text.replaceAll("\\P{InBasic_Latin}", "");

			text = text.replaceAll("[^\\x0A\\x0D\\x20-\\x7E]", "");
			// print extracted text
			// System.out.println(text);
			String sub[] = null;
			// System.out.println(extractEmail(text));

			if (text.contains("UCF Inventors")) {
				String kw = "UCF Inventors";
				inventors = GetInventorName(kw, text);
				System.out.println(inventors);

			} else if (text.contains("UCF Inventor")) {
				String kw = "UCF Inventor";
				inventors = GetInventorName(kw, text);
				System.out.println(inventors);

			} else if (text.contains("Lead Inventor")) {
				String kw = "Lead Inventor";
				inventors = GetInventorName(kw, text);
				System.out.println(inventors);
				// sub=text.substring((text.indexOf("Inventors")));

			} else if (text.contains("Inventors")) {
				String kw = "Inventors";
				// sub=text.substring((text.indexOf("Inventors")));
				inventors = GetInventorName(kw, text);
				System.out.println(inventors);

			}
			return inventors;
			// System.out.println("Name" + sub.split(",")[0]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	/*
	 * static String GetInventorName(String kw, String text) { String sub[] =
	 * null; String finalS = ""; String temp =
	 * (text.substring((text.indexOf(kw)))).replace(kw, ""); String tempsub[] =
	 * temp.split("\n"); temp = ""; for (int i = 0; i < tempsub.length; i++) {
	 * if (tempsub[i].equals("\n")) { if (i > 1) if (tempsub[i - 1].equals(kw))
	 * continue; else break; }
	 * 
	 * temp += tempsub[i]; } temp = temp.replaceAll("\n", " "); temp =
	 * temp.replaceAll("\r", " "); if (temp.toLowerCase().contains("ph.d")) { if
	 * (temp.toLowerCase().contains("and")) temp =
	 * temp.toLowerCase().replace("and", ""); sub =
	 * temp.toLowerCase().split("ph.d"); sub[sub.length - 1] = ""; } if
	 * (temp.toLowerCase().contains("phd")) { if
	 * (temp.toLowerCase().contains("and")) temp =
	 * temp.toLowerCase().replace("and", ""); sub =
	 * temp.toLowerCase().split("phd"); sub[sub.length - 1] = ""; }
	 * if(temp.toLowerCase().contains("ph. d")){ if
	 * (temp.toLowerCase().contains("and")) temp =
	 * temp.toLowerCase().replace("and", ""); sub =
	 * temp.toLowerCase().split("ph. d"); sub[sub.length - 1] = ""; } for (int i
	 * = 0; i < sub.length; i++) { sub[i] = sub[i].replaceAll(",", ""); sub[i] =
	 * sub[i].replaceAll(";", ""); if (sub[i].contains(".")) sub[i] =
	 * sub[i].replaceAll("\\.", ""); // sub[i]=sub[i].replaceAll(".",""); if
	 * (sub[i].equals("")) continue; String name[] = sub[i].trim().split(" ");
	 * // if(name.length==0) String Int[] = { "First", "Last" }; finalS +=
	 * name[0]; for (int j = 1; j < name.length; j++) { finalS += " " + name[j];
	 * 
	 * } finalS += ";"; } return finalS; }
	 */
	static String GetInventorName(String kw, String text) {
		String sub[] = null;
		String finalS = "";
		String temp = (text.substring((text.indexOf(kw)))).replace(kw, "");
		String tempsub[] = temp.split("\n");
		temp = "";
		for (int i = 0; i < tempsub.length; i++) {
			if (tempsub[i].equals("\n")) {
				if (i > 1)
					if (tempsub[i - 1].equals(kw))
						continue;
					else
						break;
			}

			temp += tempsub[i];
		}
		boolean startErase = false;
		if (sub == null) {
			sub = temp.split("\r");
			String st1 = sub[0];
			sub[0] = "";
			for (int i = 0; i < sub.length; i++) {

				if (sub[i].toLowerCase().contains("benefits")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("benifits")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("keywords")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("selected")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("publications")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("patent")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("fields")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("ucf")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("university")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("technology")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("conventional")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("contact")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("electronic")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("advantages")) {
					startErase = true;
					sub[i] = "";
				}
				if (sub[i].toLowerCase().contains("also see")) {
					startErase = true;
					sub[i] = "";
				}

				if (sub[i].matches(".*\\d+.*")) {
					startErase = true;
					sub[i] = "";
				}
				if (startErase)
					sub[i] = "";
			}
		}
		if (!startErase) {
			sub = null;
			temp = temp.replaceAll("\n", " ");
			temp = temp.replaceAll("\r", " ");
			if (temp.toLowerCase().contains("ph.d")) {
				if (temp.toLowerCase().contains("and"))
					temp = temp.toLowerCase().replace("and", "");
				sub = temp.toLowerCase().split("ph.d");
				sub[sub.length - 1] = "";
			}
			if (temp.toLowerCase().contains("ph. d")) {
				if (temp.toLowerCase().contains("and"))
					temp = temp.toLowerCase().replace("and", "");
				sub = temp.toLowerCase().split("ph. d");
				sub[sub.length - 1] = "";
			}
			if (temp.toLowerCase().contains("phd")) {
				if (temp.toLowerCase().contains("and"))
					temp = temp.toLowerCase().replace("and", "");
				sub = temp.toLowerCase().split("phd");
				sub[sub.length - 1] = "";
			}
			if (temp.toLowerCase().contains("m.p.a")) {
				if (temp.toLowerCase().contains("and"))
					temp = temp.toLowerCase().replace("and", "");
				sub = temp.toLowerCase().split("m.p.a");
				sub[sub.length - 1] = "";
			}
		}
		// System.out.println(joinArray(sub));
		finalS = extractNames(sub);
		if (finalS.contains(".")) {
			finalS = finalS.replaceAll("\\.", " ");
		}
		if (finalS.contains(",")) {
			finalS = finalS.replaceAll(",", " ");

		}
		return finalS;
	}

	static String joinArray(String sub[]) {
		String finalJoin = "";
		for (int i = 0; i < sub.length; i++) {
			finalJoin += sub[i];
		}
		return finalJoin;
	}

	static String extractNames(String names[]) {
		String finalJoin = "";
		for (int i = 0; i < names.length; i++) {
			if (names[i].contains(" and")) {
				// finalJoin += getEachNames(names[i].split(" and"));
				String ss[] = names[i].split(" and");
				if (names[i].contains(",")) {
					for (int j = 0; j < ss.length; j++) {
						finalJoin += getEachNames(ss[j].split(","));
					}
				} else {
					finalJoin += getEachNames(ss);
				}
			} else if (names[i].contains(";")) {
				if (names[i].contains(",")) {
					finalJoin += getEachNames(names[i].split(","));
				} else {

					String ss[] = names[i].split(";");
					for (int j = 0; j < ss.length; j++) {
						finalJoin += matchreplacewithin(ss[j].trim());

					}
				}
			} else {
				if (!names[i].equals("")) {
					if (names[i].contains(","))
						finalJoin += matchreplacewithin(names[i].substring(0,
								names[i].indexOf(',')) + ";");
					else {
						String ss[] = names[i].split(";");
						for (int j = 0; j < ss.length; j++) {
							finalJoin += matchreplacewithin(ss[j].trim());

						}
					}
				}
			}
		}

		return finalJoin;
	}

	static String getEachNames(String names[]) {
		String tempS = "";
		for (int i = 0; i < names.length; i++) {
			if (matchwithin(names[i]))
				continue;
			if (names[i].contains(","))
				tempS += matchreplacewithin(names[i].substring(0,
						names[i].indexOf(','))
						+ ";");
			else
				tempS += matchreplacewithin(names[i] + ";");
		}
		return tempS;

	}

	static boolean matchwithin(String temp) {
		if (temp.toLowerCase().contains("ph.d")) {
			return true;
		}
		if (temp.toLowerCase().contains("ph. d")) {
			return true;
		}
		if (temp.toLowerCase().contains("phd")) {
			return true;
		}
		if (temp.toLowerCase().contains("m.p.a")) {
			return true;
		}
		if (temp.toLowerCase().contains("ph d")) {
			return true;
		}
		return false;
	}

	static String matchreplacewithin(String temp) {
		if (temp.toLowerCase().contains("ph.d")) {
			return temp.toLowerCase().substring(0,
					temp.toLowerCase().indexOf("ph.d"))
					+ ";";
		}
		if (temp.toLowerCase().contains("ph. d")) {
			return temp.toLowerCase().substring(0,
					temp.toLowerCase().indexOf("ph. d"))
					+ ";";
		}
		if (temp.toLowerCase().contains(" phd")) {
			return temp.toLowerCase().substring(0,
					temp.toLowerCase().indexOf(" phd"))
					+ ";";
		}
		if (temp.toLowerCase().contains("m.p.a")) {
			return temp.toLowerCase().substring(0,
					temp.toLowerCase().indexOf("m.p.a"))
					+ ";";
		}
		if (temp.toLowerCase().contains("ph d")) {
			return temp.toLowerCase().substring(0,
					temp.toLowerCase().indexOf(" ph "));
		}
		return temp + ";";
	}
	/*
	 * for (int i = 0; i < sub.length; i++) { sub[i] = sub[i].replaceAll(",",
	 * ""); sub[i] = sub[i].replaceAll(";", ""); if (sub[i].contains("."))
	 * sub[i] = sub[i].replaceAll("\\.", ""); //
	 * sub[i]=sub[i].replaceAll(".",""); if (sub[i].equals("")) continue; String
	 * name[] = sub[i].trim().split(" "); // if(name.length==0) String Int[] = {
	 * "First", "Last" }; finalS += name[0]; for (int j = 1; j < name.length;
	 * j++) { finalS += " " + name[j];
	 * 
	 * } finalS += ";"; }
	 */

}
