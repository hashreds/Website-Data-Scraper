package com.tract;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Integer;
import com.opencsv.CSVWriter;



public class App {
    
    List<ArrayList<String>> dataRows;                   // Store rows to be outputted to CSV
    ChromeDriver driver;                                // Selenium driver to interact with Chrome

    public App() {
        dataRows = new ArrayList<ArrayList<String>>();
        driver = new ChromeDriver();
    }

    public static void main(String[] args) {
        
        if (args.length != 2) {                         // check for correct amount of arguments
            System.out.println("Please provide two arguments: <company OR officer> <company_name OR officer_name>");
            System.out.println("Separate parts of a name with spaces. For example:");
            System.out.println("officer Jack,McKay");
            return;
        }

        String queryName = args[1].replace(',', '+');
        String queryCategory = "";                      // set URL category query for company or officer search
        if (args[0].equals("company"))
            queryCategory = "companies";
        if (args[0].equals("officer"))
            queryCategory = "officers";
        if (queryCategory.equals("")) {
            System.out.println("Invalid Input. Please specify company or officer in first argument. For example:");
            System.out.println("java DataScraper officer Jack,McKay");
            return;
        }

        App scraper = new App();
        
        if (!processURL(queryCategory, queryName, scraper)) {        // visit the URL and look for any results
            System.out.println("No results found for " + args[0] + " " + args[1]);
            scraper.driver.quit();
            return;                                                  // if no results found, we are done
        }    
        
        
        if (queryCategory.equals("companies")) {                
            scraper.companyScrape(scraper.driver);                   // scrape data from company page
        } else {
            scraper.officerScrape(scraper.driver);                   // scrape data form officer page
        }
        
        scraper.writeData(System.getProperty("user.dir"));       // write data in dataRows to CSV in current directory
        scraper.driver.quit();
        System.out.println("Success!");
    }

    /**
     * method to open website and search using name and category
     * @param category company or officer
     * @param name name of company or officer
     * @param scraper App object to access the ChromeDriver
     * @return true if search conducted successfully and a result was clicked on, false otherwise
     */
    static boolean processURL(String category, String name, App scraper) {                                
        String urlString = "https://opencorporates.com/" + category + "?q=" + name + "&jurisdiction_code=&type=" + category;
        scraper.driver.get(urlString);                                                                          // open URL through driver using assmebled urlString
        scraper.driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));                      // wait while page loads
        List<WebElement> results = new ArrayList<>();                                                           // ArrayList to store search results
        if (category.equals("companies")) {                                                            // if company, process company search results
            String foundMsg = scraper.driver.findElement(By.cssSelector("#page_container > div.row.content.main_content > div.span7 > h2")).getText();
            int index1 = foundMsg.indexOf(" ", 0);
            int index2 = foundMsg.indexOf(" ", index1 + 1);
            int numResults = Integer.parseInt(foundMsg.substring(index1 + 1, index2));                          // get number of results
            if (numResults == 0)                                                                                // if no search results, we are done
                return false;
            WebElement resultContainer = scraper.driver.findElement(By.cssSelector("#companies"));  // find results
            results.add(resultContainer.findElement(By.xpath("//*[@id='companies']/li[1]")));   // add first result to ArrayList of results
        } else {                                                                                                // else, process officer search results
            String foundMsg = scraper.driver.findElement(By.cssSelector("#page_container > div.row.content.main_content > div.span7 > h2")).getText();
            int index1 = foundMsg.indexOf(" ", 0);
            int index2 = foundMsg.indexOf(" ", index1 + 1);
            int numResults = Integer.parseInt(foundMsg.substring(index1 + 1, index2));                          // get number of results
            if (numResults == 0)                                                                                // if no search results, we are done
                return false;
            WebElement resultContainer = scraper.driver.findElement(By.cssSelector("#results > ul")); // find results
            if (numResults <= 5) {                                                                              // if <= 5 results, scan all results for matching first name
                for (int i = 0; i < numResults; i++) {
                    WebElement listItem = resultContainer.findElement(By.xpath("//*[@id='results']/ul/li[" + (i + 1) + "]"));
                    results.add(listItem.findElement(By.tagName("a")));                                 // add result to ArrayList of results
                }
            } else {
                for (int i = 0; i < 5; i++) {                                                                   // if > 5 results, scan first five results for matching first name
                    WebElement listItem = resultContainer.findElement(By.xpath("//*[@id='results']/ul/li[" + (i + 1) + "]"));
                    results.add(listItem.findElement(By.tagName("a")));                                 // add result to ArrayList of results
                }
            }
        }
        
        if (category.equals("companies")) {                                                            // if company, get first result
            WebElement firstResult = results.get(0);    
            List<WebElement> aTags = firstResult.findElements(By.tagName("a"));                         // click on first result
            aTags.get(1).click();
        } else {                                                                                                // else, we have officers
            boolean foundName = false;
            for (WebElement result: results) {                                                                  // traverse results to check matching first name
                String resultText = result.getText();
                int indexSpace = resultText.indexOf(" ", 0);
                String resultName = resultText.substring(0, indexSpace);
                String inputName = name.substring(0, name.indexOf('+'));
                String inputNameUpperCase = inputName.toUpperCase();
                if (resultName.equals(inputNameUpperCase) || resultName.contains(".")) {                      // if found result matching first name, click
                    result.click();                                                                             
                    foundName = true;
                    break;                                                                                      // we are done
                }
            }
            if (!foundName)
                return false;
        }
        return true;
    }

    
    /**
     * collect and store company information to field dataRows 
     * @param driver to scrape company data from webpage
     */
    void companyScrape(ChromeDriver driver) {
        ArrayList<String> labels = new ArrayList<String>(Arrays.asList("Company Name", "Company Number", "Status", "Incorporation Date",
                                                                    "Dissolution Date","Company Type", "Jurisdiction", "Business Number",
                                                                    "Registry Page", "Recent Filings","Source", "Latest Events"));
        dataRows.add(labels);                                       // add column labels to dataRows
        ArrayList<String> row = new ArrayList<String>();            // ArrayList to store data for new row
        String companyName = driver.findElement(By.cssSelector("#page_container > div.row.content.main_content > div.span7 > div.vcard > h1")).getText();
        row.add(companyName);                                       // add company name to row
        String companyNum = driver.findElement(By.cssSelector("#attributes > dl > dd.company_number")).getText();
        row.add(companyNum);                                        // add company number to row
        String status = driver.findElement(By.cssSelector("#attributes > dl > dd.status")).getText();
        row.add(status);                                            // add status to row
        String incorpDate = "";
        try { incorpDate = driver.findElement(By.cssSelector("#attributes > dl > dd.incorporation_date > span")).getText();
        } catch (NoSuchElementException e) {
            System.out.println("No incorporation date was found");
        }
        row.add(incorpDate);                                        // add incorporation date to row
        String dissolDate = "";
        try { dissolDate = driver.findElement(By.cssSelector("#attributes > dl > dd.dissolution.date")).getText();
        } catch (NoSuchElementException e) {
            System.out.println("No dissolution date found");
        } 
        row.add(dissolDate);                                        // add dissolution date to row
        String companyType = "";
        try { companyType = driver.findElement(By.cssSelector("#attributes > dl > dd.company_type")).getText();
        } catch (NoSuchElementException e) {
            System.out.println("No company type found");
        }
        row.add(companyType);                                       // add company type to row
        String jurisdiction = driver.findElement(By.cssSelector("#attributes > dl > dd.jurisdiction > a")).getText();
        row.add(jurisdiction);                                      // add jurisdiction to row
        String businessNum = "";
        try { businessNum = driver.findElement(By.cssSelector("#attributes > dl > dd.business_number")).getText();
        } catch (NoSuchElementException e) {
            System.out.println("No business number found");
        }
        row.add(businessNum);                                       // add business number to row
        WebElement registryPageElement = driver.findElement(By.cssSelector("#attributes > dl > dd.registry_page > a"));
        String registryPage = registryPageElement.getAttribute("href");
        row.add(registryPage);                                      // add registry page to row
        WebElement filingsContainer = driver.findElement(By.cssSelector("#filings"));
        List<WebElement> filings = filingsContainer.findElements(By.className("filing")); // store all filings
        String allRecentFilings = "";
        for (int i = 0; i < filings.size(); i++) {                  // collect information from all filings
            String recentFiling = "";
            if (i % 2 == 0) {                                       // get date and info on even indeces
                recentFiling += filings.get(i).getText();
            }
            if (i % 2 != 0) {                                       // get link on odd indeces
                recentFiling += " ";
                recentFiling += filings.get(i).getAttribute("href");
                recentFiling += "\n";
            }                           
            allRecentFilings+=recentFiling;
            
        }
        row.add(allRecentFilings);                                  // add all recent filings to row

        String source = driver.findElement(By.cssSelector("#source > span.publisher")).getText();
        row.add(source);                                            // add source to row

        WebElement eventsContainer = driver.findElement(By.cssSelector("#events > div"));
        List<WebElement> events = eventsContainer.findElements(By.className("event-timeline-row"));
        String allLatestEvents = "";
        for (WebElement event: events) {                            // collect info from each event
            // String latestEvent = "";
            allLatestEvents += event.getText();
        }
        row.add(allLatestEvents);                                   // add all latest events to row
        dataRows.add(row);                                          // add our filled row to field dataRows
        return;
    }

    /**
     * collect and store officer information to field dataRows 
     * @param driver to navigate and scrape the officer page
     */
    void officerScrape(ChromeDriver driver) {
        ArrayList<String> labels = new ArrayList<String>(Arrays.asList("Name", "Company Name and Link", "Address",
                                                                       "Position", "Start Date", "Other Officers"));
        dataRows.add(labels);                                               // add column labels to dataRows
        ArrayList<String> row = new ArrayList<String>();                    // ArrayList to store data for new row
        String name = driver.findElement(By.cssSelector("#page_container > div.row.content.main_content > div.span7 > h1")).getText();
        row.add(name);                                                      // add name to row
        String companyNameAndLink = "";
        WebElement compContainer = driver.findElement(By.cssSelector("#attributes > dl > dd.company > a"));
        companyNameAndLink += (compContainer.getText() + " " + compContainer.getAttribute("href"));
        row.add(companyNameAndLink);                                        // add company name and link to row
        String address = driver.findElement(By.cssSelector("#attributes > dl > dd.address > a")).getText();
        row.add(address);                                                   // add address to row
        String position = driver.findElement(By.cssSelector("#attributes > dl > dd.position")).getText();
        row.add(position);                                                  // add position to row
        String startDate = "";
        try { startDate = driver.findElement(By.cssSelector("#attributes > dl > dd.start_date")).getText();
        } catch (NoSuchElementException e) {
            System.out.println("No start date found");
        }
        row.add(startDate);                                                 // add start date to row
        
        try {                                                               // other officer collection: check if there is no "see all" to click
            driver.findElement(By.cssSelector("#other_officers > h3:nth-child(1) > small > a")).click();
        } catch (NoSuchElementException e) {                                // if no "see all", collect officers shown and we are done
            System.out.println("No 'see all' button for officers found");
            WebElement officerContainer = driver.findElement(By.cssSelector("#other_officers > ul")); 
            List<WebElement> officerElts = officerContainer.findElements(By.tagName("li"));
            String officers = "";
            for (WebElement officerElt: officerElts) {
                String officerInfo = officerElt.getText();
                officers += officerInfo;
            }
            row.add(officers);
            dataRows.add(row);
            return;                                                         
        } 
                                                                            
        List<WebElement> officerElts = driver.findElements(By.cssSelector("#officers"));
        String officers = "";
        while (true) {                                                      // handle remaining pages of officers
            try {                                                           // check if next page available
                WebElement nextPageActive = driver.findElement(By.xpath("//*[@id='page_container']/div[2]/div[1]/div[2]/ul/li[4]"));
                System.out.println("nextPage found");
                officerElts = driver.findElements(By.cssSelector("#officers"));
                for (WebElement officerElt: officerElts) {                  // collect officers on current page
                    String officerInfo = officerElt.getText();
                    officers += officerInfo;
                }
                officers+="\n";

            } catch (NoSuchElementException e) {                            // if next page inactive, collect officers on current page, and we are done
                System.out.println("All officer pages visited");
                officerElts = driver.findElements(By.cssSelector("#officers"));
                for (WebElement officerElt: officerElts) {
                    String officerInfo = officerElt.getText();
                    officers += officerInfo;
                }
                break;
            } finally {                                                     // go to next page
                driver.findElement(By.cssSelector("#page_container > div.row.content.main_content > div.span7 > div.pagination > ul > li.next.next_page > a")).click();
            }
        }
        row.add(officers);                                                  // add all officers to row
        dataRows.add(row);                                                  // add our filled row to field dataRows
        return;
    }

    /**
     * write data in dataRows field to CSV
     * @param filePath path of current directory
     */
    void writeData(String filePath) {
        File file = new File(filePath +"\\data.csv");
        try {
            FileWriter outputfile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputfile);       // open writer connection to outputfile
            for (ArrayList<String> row: dataRows) {             // convert each ArrayList in dataRows to array
                String[] rowArray = new String[row.size()];     
                rowArray = row.toArray(rowArray);
                writer.writeNext(rowArray);                     // write array to CSV
            }
            writer.close();                                     // close writer connection
        }
        catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    
}
