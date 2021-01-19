# SpringBoot-Docs
Spring Boot Notes


                                                     Make War in Spring Boot:
                                                     =========================
						     
1-In your main class SpringBootServletInitializer extendsthis
2-Add the Following Depenedency In Your pom.xml 

 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-tomcat</artifactId>
 <scope>provided</scope>
 </dependency>
 
3-Change  <packaging>war</packaging> instead of  <packaging>jar</packaging>

#############################################################################################################

                                              Use Jsp in Spring boot
                                             =========================
1-Include jar in pom.xml

<dependency>
<groupId>org.apache.tomcat.embed</groupId>
<artifactId>tomcat-embed-jasper</artifactId>
<scope>provided</scope>
</dependency>

2-Create a folder inside src/main/webapp/WEB-INF/jsp
3-And make a jsp file inside that eg:- index.jsp
4-Make Sure you have following entry inside application.properties file

spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp


5-Make a controller and return that inisde that
Eg:
@RequestMapping("/first")
public String getFirstJspPage() {
System.out.println("inside first controller");
return "index";           // here index is the name of jsp file
}

##########################################################################################

                                           Use Bootstrap and and jquery using spring boot jars
                                           ===================================================

1-Paste following dependency inside your pom.xml file

<dependency>
<groupId>org.webjars</groupId>
<artifactId>bootstrap</artifactId>
<version>3.3.6</version>
</dependency>

<dependency>
<groupId>org.webjars</groupId>
<artifactId>jquery</artifactId>
<version>1.9.1</version>
</dependency>

2-Include the foolowing line inside your jsp page
  <link href="webjars/bootstrap/3.3.6/css/bootstrap.min.css"
	rel="stylesheet">              // use this inside head tag

<script src="webjars/jquery/1.9.1/jquery.min.js"></script>
<script src="webjars/bootstrap/3.3.6/js/bootstrap.min.js"></script>   //use both this script above closing of body tag </body> now you are ready to use bootstrap and jquery in jsp pages


###############################################################################

                                            How To use angular js in SpringBoot:
                                           ===================================

1-Add the following jar in pom.xml

<dependency>
<groupId>org.webjars</groupId>
<artifactId>angularjs</artifactId>
<version>1.7.0</version>
</dependency>

2-Add the following lines in your any JSP page
<script src="js/mycontroller.js"></script>
In above script js is the folder inside src/main/resources/static/js

3-Now make a file called mycontrolles.js inside that js folder and make the module and controller like below
var app=angular.module('myapp',[]);
app.controller('myctrl',function($scope){
$scope.firstname="vasu";
});

4-Now use this module and controller inside your jsp ,now you are ready to work with Angular Js
<html ng-app="myapp">
<body ng-controller = "myctrl">
<input type="text" ng-model="vasu">
<h3>{{vasu}}</h3>
</body>
</html>

#######################################################################################
Spring Boot JPA Study Link
=============================
https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#transactions
=====================================================================================================================

                                           Use H2 database 
					   with spring boot
                                           ====================================

1-Incluse folloing jar in pom.xml 
<dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
</dependency>

2-Add following lines in your application.properties file
spring.jpa.show-sql=true            //display sql logs
spring.h2.console.enabled=true          
spring.h2.console.path=/h2-console      // for h2 web-console
spring.datasource.url=jdbc:h2:mem:testdb    //In advnce version we get error to solve this we add this annotation

3-Create a entity class like below
@Entity
@Table(name = "carTable")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "NAME", nullable = false, length = 233)
    private String name;
private String lastName;
// getter-setter , default constructor, parameterized constructor

4-Now simply run the main application as java , your table will be automatically created for check thet go to below url:
http://localhost:8082/h2-console

5-Use jdbc:h2:mem:testdb as the JDBC URL ,here you ‘ll get all the tables.


#########################################################################################

                                            @jdbcTemplate in SpringBoot (For More Study Use TutorialPoint.com )
                                          =====================================================================

For using jdbcTemplate in spring boot just autowired it like below:

@Autowired
Private jdbcTemplate myTemplate;

After this use update method of this template for insert, update and delete
Syntax:
 myTemplate.update(Query comes here, parameter set object array comes here);
Eg:
 myTemplate.update(“insert into demo values(?,?,?”,new Object[]{1,”vasu”});
 
For select All:  
Mytemplate.query(“select * from demo”, new BeanPropertyRowMapper(demo));

For Select Using Id: 
Mytemplate.queryForObject(“select * from demo where id =?”,new BeanPropertyRowMApper<Person>(Person.Class)};
	
##############################################################################

                                      RowMapper in SpringBoot JDBC
                                      ============================

Suppose in above jdbc we mapp our class to return database table using BeanPropertyRowMApper, Suppose you don’t have same mapping means your bean name notmatch to your DB result so we can use the RowMapper
Synatx
=> Create a class and implements RowMapper<BeanClassName>
=> Overrides mapRow method
=> get DB return data using ResultSet of mapRow  and set it to your bean class accordingly
	
##########################################################################################

                                                              JPA
                                                              ====

Jpa (Java Persistance Api) is an interface and Hibernate a class , hibenate implements Jpa
The basic needs to connect to database in jpa are:

=> repositry
=> transaction
=> how to connect to database 

So the solution of above three is
1-@Repositry
2-@Transactional
3-Use EntityManager to get database connection in JPA, Entity manager is the interface to the persistanceContext

Eg:
@persistanceContext
EntityManager entityManager;

4-Now do jdbc task using entityManager.
Eg:
entityManager.find(demo,Ineteger);

@updaedTimeStamp and @createdTimeStamp
Generally these are the annotaion of hibernate not jpa but we can use these here
=> @updatedTiimeStamp annotaion used to modifie when we last update these row
=> @createdTimeStamp annotaion used to tell us when we create these row

Eg:
@updatedTiimeStamp
Private LocalDateTime lastUpdateDate;
@createdTimeStamp
Private LocalDateTime createdDate;

@NamedQuery and @NamedQueries
Entity manager has feature to create NamedQuery like below:
                entityManager.createNamedQuery(“”,.class);
=> but we cant’use multiple time @NamedQuery in same Entity Class
=> to prevent above we use @NamedQueries annotaion

Eg:
@NamedQueries(value={
@NamedQuery(name=””, query=””),
@NamedQuery(name=””, query=””),

@NamedQuery(name=””, query=””),
})


###############################################################################

                                                      Native Query in JPA
                                                     ====================

IF you want to use real sql query then this feature is also provided by the JPA ,so you can use the createNativeQuery method of EntityManager class

Eg:
         entityManager.createNativeQuery(“select * from demo”,demo.class);
	 
=> for parameter query
Query query =  entityManager.createNativeQuery(“select * from demo where id = ?”,demo.class);
Query.setParameter(1,123);
Or
Query query =  entityManager.createNativeQuery(“select * from demo where id = :id”,demo.class);
Query.setParameter(“id”,123);

Note: make sure when you use native query you don’use of persistancecontext


#######################################################################

                                               Crud Using Entity Manager
                                               ============================

Entity manager have merge method for both insert and update first it checks if data is present than update it if not than insert that
=> entityManager.remove method is used to delete the object
=> to select all data use JPQL(java persistance query language) 

Eg:
TypedQuery<demo> obj = entityManager.createnamedQuery(“find_all_demo”,demo.class);
Retun Obj.getResultList();
=> now create NamedQuery find_all_demo in demo @Entity Class.
	
Eg:
@Entity
@NamedQuery(“find_all_demo”,”select d from demo d”);
Public class Demo(){}

#######################################################################

                                                 Enable H2 Db console
                                                =====================

=> add below line in application.properties file

Spring.h2.console.enabled=true
Spring.jpa.properties.hibernate.generate_statistics=true
Logging.level.org.hibernate.stat=debug
Spring.jpa.show-sql=true
Logging.level.org.hibernate.type=debug   //for display parameter which set to preparestatement
Spring.jpa.properties.hibernate.format_sql=true // do not use this in production


#################################################################################3

                                       Making Crud Repositry Using Spring Boot JPA
                                       =============================================

1-Create an interface and extends the CrudRepository<EntityClassName, PrimaryKetType(eg:Integer,Long)>

@Repository
public interface CarRepositry extends CrudRepository<Car, Integer> {
}
2-Make a class annotated that with @Component and implements
CommandLineRunner and override the run method like below

@Component
public class CarCommandLine implements CommandLineRunner {
    @Autowired
    private CarRepositry carRepositry;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("############### inside car command line runner #########");
        carRepositry.save(new Car("BMW", "vasu ki hai"));
        carRepositry.save(new Car("Audi", "konika ki hai"));
        carRepositry.save(new Car("jaguar", "meri hi hai"));
    }
}

3-Simply run main class and check database you will get the entry there

NOTE:
Crud Repositry have the following methods like below:
Method	Description
long count()	Returns the number of entities
Iterable<T> findAll()	Returns all items of given type
Optional<T> findById(ID Id)	Returns one item by id
void delete(T entity)	Deletes an entity
void deleteAll()	Deletes all entities of the repository
<S extends T> save(S entity)	Saves an entity


NOTE:=> If the method returns only one item, the Optional<T> is returned instead of T.

# findBy in SpringBoot jpa :      if you want to create your own query than you can also use the findByObjectName method of SpringBootJpa
Eg:
findByName, findById, findByLastName
Here Name,Id,LastName are the object of our entity class.


Enable color logging in spring boot
For enabling the color logging in spring boot simplly add the following line in your application.properties file
spring.output.ansi.enabled=always








Other starter project like spring parents are
1- spring-boot-starter-web-service     //used to make web services using SOAP
2- spring-boot-starter-test                      //used for junit testing
3- spring-boot-starter-jdbc
4- spring-boot-starter-security
5- spring-boot-starter-data-jpa
6- spring-boot-starter-data-test
// For more google spring website there are more starter projects

Note : in many Spring Boot rest application you see that you return a list but it' ll automatically converted in JSON this process is done by MessageConverters  i.e autoconfigured in spring boot and one of them are jackson-databind jar

Eg:- List<Question> object -> JSON
JSON -> Objetc

@GetMapping : : this annotation in used for get method 
eg:
 @GetMapping("/survey/{surveyId}/questions")
  public List<Question> getSurveyNyId(@PathVariable String surveyId) {
          return serveyService.retrieveQuestions(surveyId);
    }

Devtools in SpringBoot :  InSpringboot project if we do small changes then to see that results we have to restart server so to prevent this we use devtools
dependency for Devtools

<dependency>
<groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-devtools</artifactId>
 <optional>true</optional>
</dependency>

Note : we don't use <version></version> here because it will take version automattically from spring parent

@RequestBody  :  This anntation is used to map content of request to your class objects
eg:  Suppose you have a java class and you are using @PostMapping and you send request from postman and your request data in json format then it will automatically mapp your jsonobject to your class object

syntax : 
@PostMapping("/surveys/{surveyid}/questions")
  public List<Question> addQuestionToServey(@PathVariable String surveyid , @RequestBody Question question)
{  
 }


@PostMapping : used to post data in spring boot

eg:
 @PostMapping("/surveys/{surveyid}/questions")
 public ResponseEntity addQuestionToServey(@PathVariable String surveyid, @RequestBody Question question) {
Question questionObject = serveyService.addQuestion(surveyid, question);
        // to make uri in spring boot
if (question == null) 
            return ResponseEntity.noContent().build();
URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(questionObject.getId()).toUri();
        //to make status in springboot use ResponseEntity
        return ResponseEntity.created(location).build();
    }

URL: http://localhost:8081//surveys/Survey1/questions   for postman
requestData: 

   {
    "description": "This is my custom question",
    "correctAnswer": "love",
    "options": [
      "konika",
      "oshika",
      "ankita",
      "neha"
    ]
  }


ContentNegotiation =>:   Accept:application/xml
Deliver XML responses  from Rest Service

dependency :
<dependency>
<groupId>com.fasterxml.jackson.dataformat</groupId>
<artifactId>jackson-dataformat-xml</artifactId>
</dependency>

How To Run :  Simply make a get request and go to postman and add a header
   Accept              application/xml
Now Spring Bind the result List<Question> data in xml formate instead of JSON

###########################################################################

                                 SpringBoot Actuator and Hal Browser :=>
                                ===========================================

Used For Monitoring Your Rest Services and Hal Browser provide URI around the Actuator Services
Dependencies:
<!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-rest-hal-browser</artifactId>
 </dependency>

########################################################################################

NOTE :  SpringBoot by default contains three different embedded Servers
1- tomcat          2- jetty           3-  undertow
if you want to use jetty insted of tomcat then just do the following steps:
1- do changes in your pom.xml

 <dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
    <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-tomcat</artifactId>
     </exclusion>
   </exclusions>
 </dependency>
 
2- add the dependency for jetty or undertow

<dependency>
   <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
 </dependency>

NOTE : =>  FOR MORE JUST GOOGLE HOW TO SWITCH TO UNDERTOW

###################################################################

                                  How to Use Property Value inside your service :
                                ==================================================

1- Go to applicatios.properties file and make a entry like
       welcome.message = "hello vasu chai pi lo"
       
2- TO use this property value inside you service just use @Value annotation

eg: 
@Value("${welcome.message} ")
private String myCustomMessage;
3- Now just print this anywhere System.out.println(myCustomMessage);

placeholder inside application.properties file :
app.name=i love konika
welocme.message=hi my name is vasu and ${app.name}
so now when you use the welocme.message it 'll print
hi my name is vasu and  i love konika

#####################################################################

                                     How to use YAML file in Spring Boot : 
                                   ======================================

1- For this create a new yaml file inside resources folder name application.yaml
2- make entry in yaml file like below 
       logging:
           level:
                 org.springframework :INFO
                  org.springframework.web.servlet: DEBUG
		  
 ############################################################################

                                         Profiles in SpringBoot :
                                        =========================
1- Go to application.properties file and make following entry
   spring.profiles.active=prod
   
2- make a properties file inside resources folder with name syntax applcation-your_profile_name.properties
    application.prod.properties
    
NOTE:  Now application.prod.properties has taken more prefrence than application.properties file for example if i use logging level DEBUG in application.properties and make logging level INFO in application.prod.properties then Spring Boot use INFO logging because profile take higest precedence than simple application.properties file

Usage=> use For Configure Resources - Databases, Queues, External Services


Better way For Configuration than @Value annotaion  use @ConfigurationProperties :
================================================================================

The Only Draw back is that if we have to use more properties than we have to use @Value many time So to prevent this we use the following Steps:

1- Create a class inside package configuration
2- Use the @ConfigurationProperties("basic") annotaion

@Component
@ConfigurationProperties("vasu")
public class BasicConfiguration {

    private boolean value;
    private String message;
    private int number;
//getter-setter
}

3- Go to application.properties file and make entry like below:
vasu.value=true
vasu.message= configuration works NAcho
vasu.number=123

4- Now Go to any service and make a object of Class and use the values
@RestController
public class ConfigurationController {
    @Autowired
   private BasicConfiguration basicConfiguration;
    @GetMapping("/basicConfiguration")
    public Map mysimpleConfiguration() {
        Map map = new HashMap();
        map.put("value", basicConfiguration.isValue());
        map.put("Message", basicConfiguration.getMessage());
        map.put("Number", basicConfiguration.getNumber());
        return map;
    }
}

##################################################################################

                                      Spring Boot with JPA (java persistance API) :=>
                                     ==============================================

1- For Using JPA in Spring Boot we have to use 2 jars

 <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-data-jpa</artifactId>
 </dependency>

 <!-- one of the embedde database is h2 by default present if we add this jar -->
 <dependency>
   <groupId>com.h2database</groupId
   <artifactId>h2</artifactId>
 </dependency>

2-  Now create a Entity Class inside com.vasu.JPA package

@Entity
@Table(name = "vasu")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String name;
    private String lastname;
    public User(String name, String lastname) {
        this.name = name;
        this.lastname = lastname;
    }
    public User() {
    }
// getter-setter

3- create an interface that extends CrudRepository<Name_Of_Entity_Class, Type_of_primary_key>

public interface UserRepositry extends CrudRepository<User, Integer> {
}
4-  create a class and implements CommandLineRunner, CommandLineRunner call at the time of Spring Boot application load
@Component
public class UserCommandLine implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        System.out.println("User Command Line Runner");
    }
}


########################################################

                                        Save and Get Data From SpringBoot DB Using JPA :
                                      =====================================================

1- we just use save methos of CrudRepositry to save data in DB in our case we have done the followings:
public interface UserRepositry extends CrudRepository<User, Integer> {
     List<User> findByName(String name);
}

2- Now Get SAve DAta To Display in Console

@Component
public class UserCommandLine implements CommandLineRunner {
 private static final Logger logger = LoggerFactory.getLogger("UserCommandLine.class");
@Autowired
  private UserRepositry usersrepositry;
    
    @Override
    public void run(String... args) throws Exception {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        usersrepositry.save(new User("vasu", "Rajput"));
        usersrepositry.save(new User("konika", "Rajput"));
        usersrepositry.save(new User("oshika", "Rajput"));
        logger.info("after insert all the information in db Now Print all");
        for (User user : usersrepositry.findAll()) {
            logger.info("userName= " + user.getName());
        }
        logger.info("get by name ");
        for (User user : usersrepositry.findByName("vasu")) {
            logger.info("id name= " + user.getLastname() + " firstName= " + user.getName());
        }
        System.out.println("User Command Line Runner");
    }
}

NOTE : after running this programe you can see the h2 database console on the following url
http://localhost:8082/h2-console

#######################################################################

                                              Spring Boot Security
                                            ======================
1-  Add the following dependecy
<dependency>
<groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-security</artifactId>
</dependency>

Now it will automatically apply basic authentication now if you access in any url you have to type username and password
by defult username = user
by defaault password = see your console log it will present there

###########################################################

                                     Make Custom Banner in SpringBoot
                                   ====================================

Simplly create a file name banner.txt inside resource folder and past the text that you want to display as banner, there are many website than can generate online text for your banner

################################################

                                        Select All data using @jdbcTemplate
                                     ======================================

=> 1 ) use Following code
List<Users> query = jdbctemplate.query("select * from users", new UserMapper());
for (Users u : query) {
System.out.println("name=" + u.getUsername());
}
=> 2 ) Create a class Named UserMapper which implements RowMapper<T> like below code:
public class UserMapper implements RowMapper<Users> {
@Override
public Users mapRow(ResultSet rs, int rowNumber) throws SQLException {
 System.out.println("rowNumber= " + rowNumber);
System.out.println("name= " + rs.getString("username") + " && pass= rs.getString("password") + " && domain= " + rs.getString("DOMAIN"));
Users user = new Users();
 user.setUsername(rs.getString("username"));
user.setPassword(rs.getString("password"));
user.setDomain(rs.getString("DOMAIN"));
return user;
}
}

Select Single data By Id Using @jsbcTemplate
=>1) use the below Code 
String queryForObject = jdbctemplate.queryForObject("select username from users where username = ?", new Object[]{id}, String.class);


Application.properties
# Connection settings
 
spring.datasource.url=  
spring.datasource.username= 
spring.datasource.password= 
spring.datasource.driver-class-name= 
 
# SQL scripts to execute
 
spring.datasource.schema=  
spring.datasource.data= 
 
# Connection pool settings
 
spring.datasource.initial-size=  
spring.datasource.max-active= 
spring.datasource.max-idle= 
spring.datasource.min-idle=

#some other config
server.port=9000 
server.address=192.168.11.21 
server.session-timeout=1800 
server.context-path=/accounts 
server.servlet-path=/admin 

# For complete list of application.properties follow the given link
https://docs.spring.io/spring-boot/docs/2.0.2.RELEASE/reference/htmlsingle/#common-application-properties

#Rename application.properties in spring boot
@SpringBootApplication 
public class MasteringSpringBootApplication { 
 
   public static void main(String[] args) { 
         System.setProperty("spring.config.name", "myapp"); 
         SpringApplication.run(MasteringSpringBootApplication.class, args); 
   } 
}

@configurationProperties
This annotation is used to create custom properties for application.properties file using a Java bean class
Eg:
 @Component 
@ConfigurationProperties(prefix="accounts.client") 
public class ConnectionSettings { 
   private String host; 
   private int port; 
   private String logdir; 
   private int timeout; 
   ... 
   // getters/setters 
   ... 
}


This POJO defines the following properties in the application.properties file:
accounts.client.host=192.168.10.21 
accounts.client.port=8181 
accounts.client.logdir=/logs 
accounts.client.timeout=4000 

NOTE:
 Don't forget to add @EnableConfigurationProperties in one of your Spring configuration classes because the @ConfigurationProperties annotation won't work unless you have enabled it by adding the @EnableConfigurationProperties annotation.

#########################################################################

                                         # Post Data To SpringBoot Rest Using AJAX
                                        ============================================
Steps:
1-Create a Rest Api And Annotate that with @PostMapping and take parameter as @RequestBody BeanClassName BeanClassObj
2-Create a Bean Class like any name I give it student with getter-setter
3-Write Ajax Script To Call End Point and Post Data
Implementation Of Steps:

1-
@PostMapping("/rest/ajax")
public ResponseEntity ajaxDemo(@RequestBody FormBean payload, HttpServletRequest request) {  // code come here}

NOTE:= you can get path for ajax post using request.getRequestURI() method if you get 404 on post using Ajax.

2-
public class FormBean {
 private String name;
private String email;
private String subject;
private String query;
//getter-setter default parameterized and toString method

3-
function onFormSubmit() {
    console.log("ajax working");
    var form = {};
    form["name"] = $('#name').val();
    form["email"] = $('#email').val();
    form["subject"] = $('#subject').val();
    form["query"] = $('#query').val();
    $.ajax({
        type: 'POST',
        url: '/Portfolio/rest/ajax',                      // project_name/RestPoint
        cache: false,
        contentType: "application/json",
        data: JSON.stringify(form),
        timeout: 30000,
success: function (data, textStatus, jqXHR) {
console.log("success");
}, error: function (jqXHR, textStatus, errorThrown) {
console.log("error");
}
});
}



########################### Different Ways To Make Post Request In Spring Boot ###########################

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger("RequestController.class");
	JSONParser parser = new JSONParser();

	@GetMapping("/simple")
	public String test() {
		return "success";
	}

	// url will be [ http://localhost:8080/requestDemo/param/131 ]
	@GetMapping("/param/{orgid}")
	public String paramAttributeDemo(@PathVariable("orgid") String oId) {
		logger.info("orgId= " + oId);
		return "success";
	}

	/*
	 * url = http://localhost:8080/requestDemo/requestParam/ 
	 * select x-www-form-urlencoded on postman
	 *  key = id value = anythng
	 *  basicaly used for getting html form name attribute value
	 */
	@PostMapping("/requestParam")
	public String requestParamDemo(@RequestParam("id") String id) {
		logger.info("id= " + id);
		return "success";
	}

	// Use Model class For Mapping for Send(POST) The String as Json here keys name
	// must be same as Model Object otherwise it will print 0 as value
	/*
	 * simplly send(POST) the below data { "totalorganization":"", "totaldevice":"",
	 * "totalauthentication":"123" } //make sure you select [application/json
	 * formate]
	 * 
	 */
	@PostMapping("/requestModel")
	public String requestModelDemo(@RequestBody DashboardModel demo) {
		logger.info("Request= " + demo.getTotalauthentication());
		return "success";
	}

	// For Simplly Send The String as Json No bound on keys
	/*
	 * Used This When You accept only json without any key
	 * simplly send(POST) the below data { "totalorganization":"", "totaldevice":"",
	 * "totalauthentication":"123" }
	 * 
	 */
	@PostMapping("/requestModel1")
	public String requestModelDemo1(@RequestBody String payload) {
		logger.info("Request payload");
		try {
			logger.info("*** before ***");
			logger.info(payload);
			JSONObject jobj = (JSONObject) parser.parse(payload);
			logger.info("*** after ***");
			logger.info(jobj.toJSONString());
			logger.info(jobj.get("totalauthentication").toString());
		} catch (Exception e) {
			logger.error("Error", e);
		}

		return "success";
	}
	
######################################## END #######################################################


################################### Upload And Get Image In Spring Boot ############################
Steps:
1- Simplly create a POJO Class and Make a field byte[] image and annotated it with annotation @Lob
Eg:
==
        @Lob
	@Column(name = "Image")
	private byte[] image;
	
2- We Use JPA To Save and Retrieve Image In This Example
3- Make Service , DAO interface that extends CrudRepositry And Controolers.
4- Make Controller like below
=============================

// When We HardCode the File Location
=====================================
@GetMapping("/saveImage")
	public ImageHibernate saveImageController() {
		logger.info("***** SaveImageController ******");
		try {
			File file = new File("C:\\Users\\rv\\Documents\\IMG-20171111-WA0010.jpg");
			byte[] saveImageFile = new byte[(int) file.length()];
			ImageHibernate imageHibernate = new ImageHibernate();
			imageHibernate.setName("vasu image");
			imageHibernate.setImage(saveImageFile);

			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(saveImageFile);
			fileInputStream.close();
			
			String saveImageService = imageService.saveImageService(imageHibernate);
			logger.info("saveStatus= " + saveImageService);
			byte[] encode = java.util.Base64.getEncoder().encode(saveImageFile);
			logger.info("realImage= " + new String(encode));
			// Reterive Image
			imageHibernate.getImage();
			return imageHibernate;
		} catch (Exception e) {
			logger.info("inside main Exception");
			logger.error("Error", e);
			return null;
		}
	}

// When Post Image Using Form Or Postman
//if send from postman then select form-data radio-button and give key name and select key as file then upload file
===========================================

	@PostMapping("/saveImageByPost")
	public ImageHibernate saveImageUsingPost(@RequestParam("file1") MultipartFile file) {
		logger.info("Post File ");
		try {
			logger.info("DataFilename= "+file.getOriginalFilename());
			logger.info("Extension= "+ file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));
			logger.info("bytes= " + file.getBytes());
			byte[] encode = java.util.Base64.getEncoder().encode(file.getBytes());
			logger.info("realImage= " + new String(encode));
			
			ImageHibernate imageHibernate = new ImageHibernate();
			imageHibernate.setName("koku image");
			imageHibernate.setImage(file.getBytes());
			String saveImageService = imageService.saveImageService(imageHibernate);
			
			//reterive image
			logger.info("fileData= "+new String(imageHibernate.getImage()));
			byte[] encode1 = java.util.Base64.getEncoder().encode(imageHibernate.getImage());
			logger.info("realImageGetting= " + new String(encode1));
			// Reterive Image
			imageHibernate.getImage();
			return imageHibernate;
		} catch (Exception e) {
			logger.error("Error", e);
		}

		return null;

	}

5- Make Service Like Below To Save Images
========================================
@Service
public class ImageService {

	@Autowired
	private ImageDao imageDao;

	private static final Logger logger = LoggerFactory.getLogger("ImageService.class");

	public String saveImageService(ImageHibernate imageHibernateObj) {
		try {
			imageDao.save(imageHibernateObj);
			Iterable<ImageHibernate> findAll = imageDao.findAll();
			for (ImageHibernate hibernate : findAll) {
				logger.info("fileName= " + hibernate.getName() + " && id= " + hibernate.getId());
				logger.info("byteCode= "+ hibernate.getImage());
				byte[] encodeImage = java.util.Base64.getEncoder().encode(hibernate.getImage());
				logger.info("imageByte= " + new String(encodeImage));
			}
			return "succesfully Save Image";
		} catch (Exception e) {
			logger.error("Error", e);
			return "Error in image insertion";
		}
	}
}


########################################## END ####################################################################

############################################# SSL Certificate in Spring Boot ####################################

FollowLink: - https://github.com/spring-projects/spring-boot/issues/9836
Steps:
1- go to your java install location in your c drive and then go to jre and see there is a keytool , now open cmd here and type below command, and save this certificate inside your resource folder of your project

keytool -genkey -keystore C:\Users\rv\Desktop\Certificatekeystore.jks -alias tomcat -keyalg RSA -keysize 4096 -validity 720

2- Go to application.properties and do following Entry

# for https
server.port=8442
server.ssl.key-store=classpath:vasuCertificate.jks
server.ssl.key-store-password=vasurajput
server.ssl.key-password=vasurajput

3- Create a @Configuration Class like below 

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vasu.HttpsDemo.Controller;

import java.io.File;
import java.io.IOException;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author Vasu Rajput
 */
//stackoverflow link
//https://github.com/spring-projects/spring-boot/issues/9836
@Configuration
public class Https {

    @Bean
    public ServletWebServerFactory servletContainer() {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
//        return tomcat;
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        return tomcat;
    }

    private Connector createStandardConnector() {
//        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
//        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
//        try {
//            File keystore = new ClassPathResource("vasuCertificate").getFile();
//            File truststore = new ClassPathResource("vasuCertificate").getFile();
//            connector.setScheme("https");
//            connector.setSecure(true);
//            connector.setPort(8443);
//            protocol.setSSLEnabled(true);
//            protocol.setKeystoreFile(keystore.getAbsolutePath());
//            protocol.setKeystorePass("changeit");
//            protocol.setTruststoreFile(truststore.getAbsolutePath());
//            protocol.setTruststorePass("changeit");
//            protocol.setKeyAlias("apitester");
//            return connector;
//        } catch (IOException ex) {
//            throw new IllegalStateException("can't access keystore: [" + "keystore"
//                    + "] or truststore: [" + "keystore" + "]", ex);
//        }
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8442);
        return connector;
    }
}


Now When you run your application on port 8080 it will redirect to 8442 that is ssl port

######################################## END #########################################################################

###################################### Hit Api Using AngularJs Http ##########################################
1- Make A Controller like below
================================

app.controller("secondController", function ($scope, $http) {
    $(function () {
        alert("second controller");
        $http({
            url: "getStudentDetailController",
            method: "GET"
        }).then(function (response) {
            console.log("**** Success Result ****");
            console.log(response);
            $scope.name = response.data.name;
            $scope.lasname = response.data.lastName;
            console.log("name= " + $scope.name + " lastName= " + $scope.lasname);
        }, function (error) {
            console.log("**** Error Result ****");
            console.log(error);
        });
    });
});

2- Make A Controller like below
================================

    @GetMapping("/getStudentDetailController")
    @ResponseBody                         // it is must otherwise you will get server error because it will return jsp page otherwise
    public String demo() {
        logger.info("Call Get All Students Detail");
        JSONObject jobj = new JSONObject();
        jobj.put("name", "vasu");
        jobj.put("lastName", "rajput");
        return jobj.toJSONString();
    }
    
############################################## END ######################################
########################## MYSQL basic Properties ###############################

# database connection
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:mysql://localhost:3306/vasu
spring.datasource.username=root
spring.datasource.password=12345@
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.MySQL5Dialect
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
hibernate.hbm2ddl.auto=create-drop

# for logging
logging.level.org.springframework.web=ERROR
logging.level.com.vasu=DEBUG

# Logging pattern for the console
logging.pattern.console= "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} %L- %msg%n"

# Logging pattern for file
logging.pattern.file=="%d{HH:mm:ss.SSS} [%thread] %-5level  %class{36}.%M %L  - %msg%n"


# use For Exact Naming Of Column and Table in Hibernate otherwise hibernate add _ in camelcase names
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl


=============================================================================
# OneToOne Mapping
===================
Address.class

@Entity
@Table(name = "Address")
public class Address {

    @Id
    @Column(name = "AddressId")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long addressId;

    @Column(name = "City")
    private String city;

    @Column(name = "Name")
    private String state;

    @Column(name = "Country")
    private String country;

    @Column(name = "PinCode")
    private String pinCode;

    @OneToOne(targetEntity = Employee.class)
    private Employee employee;
    
    
 Employee.class
 =================
 @Entity
@Table(name = "Employee")
public class Employee {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "MOBILE_NUMBER")
    private String mobileNumber;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "EMPLOYEE_IMAGE")
    private byte[] employeeImage;

    @OneToOne(targetEntity = Address.class, cascade = CascadeType.ALL)
    private Address address;
    
  OUTPUT:
  ========
  mysql> desc employee;
+-------------------+--------------+------+-----+---------+-------+
| Field             | Type         | Null | Key | Default | Extra |
+-------------------+--------------+------+-----+---------+-------+
| ID                | bigint(20)   | NO   | PRI | NULL    |       |
| EMAIL             | varchar(255) | YES  |     | NULL    |       |
| EMPLOYEE_IMAGE    | tinyblob     | YES  |     | NULL    |       |
| MOBILE_NUMBER     | varchar(255) | YES  |     | NULL    |       |
| NAME              | varchar(255) | YES  |     | NULL    |       |
| address_AddressId | bigint(20)   | YES  | MUL | NULL    |       |
+-------------------+--------------+------+-----+---------+-------+      
mysql> desc address;
+-------------+--------------+------+-----+---------+-------+
| Field       | Type         | Null | Key | Default | Extra |
+-------------+--------------+------+-----+---------+-------+
| AddressId   | bigint(20)   | NO   | PRI | NULL    |       |
| City        | varchar(255) | YES  |     | NULL    |       |
| Country     | varchar(255) | YES  |     | NULL    |       |
| PinCode     | varchar(255) | YES  |     | NULL    |       |
| Name        | varchar(255) | YES  |     | NULL    |       |
| employee_ID | bigint(20)   | YES  | MUL | NULL    |       |
+-------------+--------------+------+-----+---------+-------+


=================================================================================================================
#OneToManyMapping
===================
Question.class
=============
@Entity
@Table(name = "Questions")
public class Question {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.TABLE)
    private long id;

    @Column(name = "QuestionName")
    private String qName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "qid")
    @OrderColumn(name = "type")
    private List<Answer> answer;
    
 Answers.class
 =============
 @Entity
@Table(name = "Answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "ID")
    private long id;

    @Column(name = "AnswerName")
    private String answerName;

    @Column(name = "PostedBy")
    private String postedBy;
    
    OUTPUT:
    ===========
    mysql> desc questions;
+--------------+--------------+------+-----+---------+-------+
| Field        | Type         | Null | Key | Default | Extra |
+--------------+--------------+------+-----+---------+-------+
| ID           | bigint(20)   | NO   | PRI | NULL    |       |
| QuestionName | varchar(255) | YES  |     | NULL    |       |
+--------------+--------------+------+-----+---------+-------+

mysql> desc answers;
+------------+--------------+------+-----+---------+-------+
| Field      | Type         | Null | Key | Default | Extra |
+------------+--------------+------+-----+---------+-------+
| ID         | bigint(20)   | NO   | PRI | NULL    |       |
| AnswerName | varchar(255) | YES  |     | NULL    |       |
| PostedBy   | varchar(255) | YES  |     | NULL    |       |
| qid        | bigint(20)   | YES  | MUL | NULL    |       |
| type       | int(11)      | YES  |     | NULL    |       |
+------------+--------------+------+-----+---------+-------+


=========================================================================================================
#ManyToOne
=================
Employee.class
=================

@Entity
@Table(name = "Employee1")
public class Employee1 {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "EmployeeId")
    private long employeeId;

    @Column(name = "Name")
    private String name;

    @Column(name = "Email")
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Address1 address;
    
    =======================================================================
  #Address.class
  ====================
  
  @Entity
@Table(name = "Address")
public class Address {

    @Id
    @Column(name = "AddressId")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long addressId;

    @Column(name = "City")
    private String city;

    @Column(name = "Name")
    private String state;

    @Column(name = "Country")
    private String country;

    @Column(name = "PinCode")
    private String pinCode;

    @OneToOne(targetEntity = Employee.class)
    private Employee employee;
    
 ===================================================
 OUTPUT
 ==========
 
 mysql> select * from address1;
+----+-------+---------+-------+---------------------+
| ID | City  | Country | State | employee_EmployeeId |
+----+-------+---------+-------+---------------------+
|  7 | Delhi | India   | Delhi |                NULL |
+----+-------+---------+-------+---------------------+
1 row in set (0.00 sec)

mysql> select * from employee1;
+------------+------------------+---------------+------------+
| EmployeeId | Email            | Name          | address_ID |
+------------+------------------+---------------+------------+
|          6 | vasu@gmail.com   | Vasu Rajput   |          7 |
|          8 | konika@gmail.com | Konika Rajput |          7 |
+------------+------------------+---------------+------------+

============================================================================
#ManyToMany
=============
Note:- Here three table created one extra table for get info of question and ans table

Question.class
==============
@Entity
@Table(name = "Question1")
public class Question1 {

    @Id
    @Column(name = "QId")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long qid;

    @Column(name = "QName")
    private String qname;

    @ManyToMany(targetEntity = Answer1.class, cascade = CascadeType.ALL)
    @JoinTable(name = "Questions_Answers",
            joinColumns = {
                @JoinColumn(name = "Q_Id"),},
            inverseJoinColumns = {
                @JoinColumn(name = "ANS_ID")})
    private List<Answer1> answerList;
    
 ===============================================
 Answer.class
 ============
 
 
 	@Entity
@Table(name = "Answer1")
public class Answer1 {

    @Id
    @Column(name = "AnsId")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long ansId;

    @Column(name = "Answer")
    private String answer;

    @Column(name = "PostedBy")
    private String postedBy;
    
 =======================================================
 #Output
 =============
 
 mysql> desc question1;
+-------+--------------+------+-----+---------+-------+
| Field | Type         | Null | Key | Default | Extra |
+-------+--------------+------+-----+---------+-------+
| QId   | bigint(20)   | NO   | PRI | NULL    |       |
| QName | varchar(255) | YES  |     | NULL    |       |
+-------+--------------+------+-----+---------+-------+
2 rows in set (0.01 sec)

mysql> desc answer1;
+----------+--------------+------+-----+---------+-------+
| Field    | Type         | Null | Key | Default | Extra |
+----------+--------------+------+-----+---------+-------+
| AnsId    | bigint(20)   | NO   | PRI | NULL    |       |
| Answer   | varchar(255) | YES  |     | NULL    |       |
| PostedBy | varchar(255) | YES  |     | NULL    |       |
+----------+--------------+------+-----+---------+-------+
3 rows in set (0.01 sec)

mysql> desc questions_answers;
+--------+------------+------+-----+---------+-------+
| Field  | Type       | Null | Key | Default | Extra |
+--------+------------+------+-----+---------+-------+
| Q_Id   | bigint(20) | NO   | MUL | NULL    |       |
| ANS_ID | bigint(20) | NO   | MUL | NULL    |       |
+--------+------------+------+-----+---------+-------+
2 rows in set (0.01 sec)


=========================== Send Email In Spring Boot ===========================
================================================================
#Steps1:  Add This dependency in pom.xml
=======
 <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
  </dependency>
  
#Step 2: Go to application.properties file and write following property
========

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=Your Email Id 
spring.mail.password=*********
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

#Step3: create a service class and paste the code
=======
@Autowired
JavaMailSender mailSender;

MimeMessage message = mailSender.createMimeMessage();
MimeMessageHelper messageHelper = new MimeMessageHelper(message);
messageHelper.setTo("demo@demo.com");
messageHelper.setSubject("Testing");
messageHelper.setText("Nice Try");
mailSender.send(message);
logger.info("succesfullySendEmail");



=============================== Enable Swagger in spring boot ==========
========================================================================
Basically swagger2 is used for Documentation in Spring boot

Steps:
Step1: Add Below dependency in pom.xml
=========

<dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
            <version>2.8.0</version>
</dependency>
		
        <!-- https://mvnrepository.com/artifact/io.springfox/springfox-swagger-ui -->
<dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
            <version>2.8.0</version>
</dependency>

Step2: Enable swaager using @EnableSwagger2 annotation and create a docker for your base package that will show all the Api in Documentation like below:

@SpringBootApplication
@EnableScheduling
@EnableSwagger2
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SPRING_WEB.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.basePackage("com.vasu.Scheduler")).build();
    }

}


#Step3 : Now go to http://localhost:8080/swagger-ui.html url you will get your complete Api Documnetation Here
==========


#NOTE:- Suppose you want to use two differnet end points with same name then use params attribute as follows
=================================================

@RequestMapping(value = "/login", method = RequestMethod.GET, params = "error")
 @GetMapping(value = "/login", params = "")
 
 # Jstl JSP tag in spring boot
 =============================
 Just add below dependency and you can use jstl libraries
 
  <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
  </dependency>
  
  
 # For log4j in spring boot follow below steps:
 =====================================
 1- add below dependency 
 
  <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
            <version>1.2.16</version>
 </dependency>
 
 2- create file log4j.properties file in class path and resource file and make following entry
 
#log4j.rootCategory=INFO, A1
#log4j.appender.A1=org.apache.log4j.FileAppender
#log4j.appender.A1.File=/var/log/vasu.log             For Windows Path will be like C:\\Users\\vasurajput\\Desktop\\vasu.log         
#log4j.appender.A1.layout=org.apache.log4j.PatternLayout
#log4j.appender.A1.layout.ConversionPattern=%d{DATE} %F|%L : %m%n


=> For Rolling Files with size use below properties
====================================================
log4j.rootCategory=INFO, A1
log4j.appender.A1=org.apache.log4j.RollingFileAppender
log4j.appender.A1.File=/home/vasurajput/Videos/vasu.log
log4j.appender.A1.layout=org.apache.log4j.PatternLayout
log4j.appender.A1.layout.ConversionPattern=%d{DATE} %F|%L : %m%n
log4j.appender.A1.MaxFileSize=3KB
log4j.appender.A1.MaxBackupIndex=2


########################## Eureka Server #######################
1- Add below dependency

                 <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
		</dependency>
		
2- Enable it in your main class using below annotation

@SpringBootApplication
@EnableEurekaServer

3- Make Below Entry in your application.properties file

spring.application.name=netflix-eureka-server
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false


########################## Eureka Client #######################
1- Add below dependency 

               <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
			<version>2.1.1.RELEASE</version>
		</dependency>
		
2- Enable it in your main class using below annotation

@SpringBootApplication
@EnableEurekaClient

3- Give Eureka Server Url in your application.properties file

eureka.client.service-url.default-zone=http://localhost:8761/eureka

########################## API GATEWAY (Zull Server ) #######################
Use as a Filter

1- Add below dependency

                <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
		</dependency>
 
2 - Enable in your main Class Using below annotations

@SpringBootApplication
@EnableZuulProxy
@EnableDiscoveryClient

3- Make below entry in your application.properties file

spring.application.name=netflix-zull-api-gateway-server
server.port=8765
eureka.client.service-url.default-zone=http://localhost:8761/eureka

For Looging Example Using Zull
=============================
1- create a class and extends ZuulFilter

@Component
public class ZullLoggingFilter extends ZuulFilter {
	
	private static final Logger logger = LoggerFactory.getLogger(ZullLoggingFilter.class);


	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() throws ZuulException {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		logger.info("vvv::  requst="+request + " ,uri= "+request.getRequestURI());
		return null;
	}

	@Override
	public String filterType() {
		return "pre";               //May be "pre","post","error"
	}

	@Override
	public int filterOrder() {
		return 1;
	}

}

2- To execute application using Zull we have to hit URL in given formate
     
     htttp://localhost:{ZULL_PORT}/{CLIENT_SERVICE_NAME}/{CLIENT_URL}
     Eg=>  http://localhost:8765/currency-exchange-service/currency-exchange/from/AUD/to/INR
     
     
 ############### Installing RabbitMQ ######################

For Installing RabbitMQ we have To Install "ER Lang" First

1- sudo apt-get update
2- sudo apt-get install erlang

3- sudo apt-get install rabbitmq-server
4- sudo systemctl enable rabbitmq-server 
5- sudo systemctl start rabbitmq-server 
6- sudo systemctl status rabbitmq-server 

RabbitMq provide Nice Dashboard for it we have to enable Plugins using command

7- sudo rabbitmq-plugins enable rabbitmq_management

open Now:  http://localhost:15672

By Default
 username=guest
 password=guest

# For Creating your own user

8- sudo rabbitmqctl add-user admin[This is userName] admin[ This is password]      
9- sudo rabbitmqctl set_user_tags admin administrator   
10- sudo rabbitmqctl set_permissions -p / admin "." "." "." 

No again Login 
  username - admin
  password - admin
  
  
  ################## Message With Redis Server #####################
  1- go to src folder of redis server
  2- Run command ./redis-server --protected-mode no 
  Note:- we use --protected-mode no  command because we don't provide user name in our application without this our application will make error
  3- Make Below entry in your application.properties file
  =========================================================
  server.port=9001
  spring.redis.host=191.96.12.46
  spring.redis.port=6379

  4- Follow below URL
  https://spring.io/guides/gs/messaging-redis/
  
  ################################## Docker Install in centos 7 ###################################
  use below URL to install docker in cento 7
  https://stackoverflow.com/questions/28483379/installing-docker-on-centos-6-6
  
  For ubuntu
  sudo apt install docker.io  
  
  ######################### Microserice Logging  using ELK ####################################
  1- Install Elastic Search using below URL
      https://www.digitalocean.com/community/tutorials/how-to-install-and-configure-elasticsearch-on-centos-7
      https://www.tecmint.com/install-elasticsearch-logstash-and-kibana-elk-stack-on-centos-rhel-7/
   
  After installing check status using 
    sudo service elasticsearch status 
  if stopped then statrt and go to URL http://localhost:9200/
  
  
  ###########################  OAuth 2 in Spring Boot #############################
  To Get Access Token
  =================
  1- Add below dependency in pom.xml file
  
                <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.security.oauth</groupId>
			<artifactId>spring-security-oauth2</artifactId>
			<version>2.0.10.RELEASE</version>
		</dependency>
		
 2- create class SpringSecurityConfiguration and extends WebSecurityConfigurerAdapter
 
    @Configuration
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		UserDetails user = User.builder().username("user").password(passwordEncoder.encode("secret")).roles("USER")
				.build();
		UserDetails userAdmin = User.builder().username("admin").password(passwordEncoder.encode("secret"))
				.roles("ADMIN").build();
		return new InMemoryUserDetailsManager(user, userAdmin);
	}

//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
//	}

}

3- create class AuthorizationServerConfig and extends AuthorizationServerConfigurerAdapter

   @Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private TokenStore tokenStore;

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory().withClient("client")
				.authorizedGrantTypes("password", "authorization_code", "refresh_token", "implicit")
				.authorities("ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "USER").scopes("read", "write").autoApprove(true)
				.secret(passwordEncoder().encode("password"));
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints.authenticationManager(authenticationManager).tokenStore(tokenStore);
	}

	@Bean
	public TokenStore tokenStore() {
		return new InMemoryTokenStore();
	}

}


4-  Open Postman 
       First Choose Authorization -> Basic Auth -> give client UserName and Passowrd from Authorization class
       Secondly Choose body -> form-data -> POST
       key         value
       grant_type   password
       username     your username from websecurity class
       password     password from websecurity class
       
       
############################# Spring Boot Security With MYSQl Security ###############
Follow My Git URL fot this
https://github.com/vasurajput/SpringBootSecurity/tree/master/SpringBootSecurityMYSql


#################################  Install Zipkin in Ubuntu #########################
curl -sSL https://zipkin.io/quickstart.sh | bash -s

