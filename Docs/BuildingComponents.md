Individual components to build
Module 1: Authentication and User Management
ID	Component	Responsibility
C01	User Registration	Register recruiters and candidates
C02	User Authentication	Login, logout and session management
C03	Role-Based Access Control	Restrict recruiter and candidate operations
C04	User Profile Management	Manage user information

Output: Authenticated recruiter and candidate accounts with role-based access.
Module 2: Workspace and Repository Management
ID	Component	Responsibility
C05	Workspace Management	Create and manage assessment workspaces
C06	GitHub Repository Integration	Accept repository URL and branch
C07	Repository Cloning	Clone the selected repository
C08	Repository Validation	Verify Java, Spring Boot and build configuration
C09	Repository Storage	Store repository snapshots and metadata

Output: A validated and accessible Spring Boot repository.
Module 3: Repository Analysis Engine
ID	Component	Responsibility
C10	Project Structure Analyzer	Identify packages, folders and project structure
C11	Java AST Parser	Parse Java source files using JavaParser
C12	API Endpoint Extractor	Extract controllers, HTTP methods and endpoints
C13	Code Relationship Analyzer	Identify relationships between controllers, services, repositories and entities
C14	Analysis JSON Generator	Convert extracted information into structured JSON

Output: Structured repository analysis that can be consumed by the AI module.

Example output:
{
  "controllers": [],
  "endpoints": [],
  "services": [],
  "repositories": [],
  "entities": [],
  "dependencies": []
}


Module 4: AI-Based Assessment Generation
ID	Component	Responsibility
C15	Feature Suggestion Engine	Generate new API-level feature suggestions
C16	Feature Validation Engine	Check feasibility and detect duplicate APIs
C17	Feature Specification Generator	Generate requirements, API contracts and acceptance criteria
C18	Hidden Test Case Generator	Generate positive, negative, boundary and edge-case tests
C19	Assessment Review and Approval	Allow recruiters to review and approve generated assessments

Output: An approved assessment containing an implementation specification and hidden HTTP test cases.
Module 5: Assessment and Candidate Management
ID	Component	Responsibility
C20	Assessment Management	Create, publish and manage assessments
C21	Exam Code Generator	Generate unique exam codes
C22	Candidate Assignment	Assign individual assessments to candidates
C23	Candidate Assessment Portal	Validate exam codes and display assessment instructions

Output: A published assessment accessible to its assigned candidate.
Module 6: Candidate Development Environment
ID	Component	Responsibility
C24	Browser-Based Code Editor	Integrate Monaco Editor
C25	Repository File Explorer	Display repository folders and files
C26	File Editing and Management	Create, modify and save source files
C27	Submission Manager	Capture and store the candidate's single final submission

Output: A complete source-code snapshot submitted by the candidate.
Module 7: Code Execution and Evaluation Engine
ID	Component	Responsibility
C28	Execution Workspace Manager	Prepare an isolated execution workspace
C29	Docker Container Manager	Create and manage restricted containers
C30	Build Execution Engine	Execute Maven/Gradle builds
C31	Application Startup Manager	Start the Spring Boot application
C32	HTTP Test Runner	Execute hidden API test cases
C33	Test Result Processor	Capture pass, fail, error and execution time
C34	Scoring Engine	Calculate assessment scores
C35	Evaluation Report Generator	Generate final evaluation reports

Output: Execution logs, test results, score and evaluation report.
Module 8: Results and Reporting
ID	Component	Responsibility
C36	Recruiter Results Dashboard	Display candidate assessment results
C37	Candidate Feedback Dashboard	Display permitted feedback and submission status
C38	Assessment Analytics	Show test pass rates, scores and performance summaries