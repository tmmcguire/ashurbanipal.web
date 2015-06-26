# ashurbanipal.web
Java Servlet-based interface to the Ashurbanipal data.

Exploring Project Gutenberg with natural language processing.

## DESCRIPTION

This project supplies the processing capacity to the
ashurbanipal.web.ui front-end. The project uses [Apache
Wink](https://wink.apache.org/) for no very good reason, but includes
four resources supplying five URLs:

1. FileMetadataLookup.java

   * /lookup: Query the Project Gutenberg metadata to provide a list
     of matching texts. Query processing, idiotic as it is, is based
     loosely on the book [*Introduction to Information
     Retrieval*](http://www-nlp.stanford.edu/IR-book/), by Christopher
     D. Manning, Prabhakar Raghavan, and Hinrich Schütze.

   * /lookup/{etext_no}: Return the metadata associated with a single
     Project Gutenberg text. This is needed when using a URL similar
     to http://dpg.crsr.net/#773 or the forward/back history support.

2. FileCombinationRecommendations.java, FileStyleRecommendations.java,
   and FileTopicRecommendations.java: Provide lists of recommendations
   for a supplied etext number.

   * /combination

   * /style

   * /topic

Data for the application is stored in tab-separated text files, for
easy reading by the application on start-up. While the application is
running, it is all stored in memory to improve response time.

## SEE ALSO

* [ashurbanipal.web.ui](https://github.com/tmmcguire/ashurbanipal.web.ui): Javascript client UI to the ashurbanipal.web interfaces.

* [ashurbanipal](https://github.com/tmmcguire/ashurbanipal): Applications to generate the data set on which recommendations are based.

## AUTHOR

Tommy M. McGuire wrote this.

## LICENSE

GNU GPLv2 or later.
