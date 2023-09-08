# Website Data Scraper

### Goal: 
To gain experience with web scraping and interacting with web pages programmatically. *This code is for educational purposes only and is not to be used for any other purpose.*

### Functionality:
My program takes a company name or the name of a company officer as arguments and stores relevant information about the company or company officer into a .csv file.
1. Company name or name of a company and its officer is supplied to the program by the user.
2. A headless browser is launched through Selenium API to navigate the opencorporates.com user interface and find any available information.
3. Available and relevant information is stored, and the browser is closed.
4. The stored information is written to a .csv file through OpenCSV API.
