Evidence Project – Functional Requirements Summary
Evidence is an AI-powered technical assessment platform that evaluates candidates by asking them to implement new features in their existing Spring Boot repositories and automatically grading their implementations through hidden API tests.
1. Recruiter Management
- Recruiter registration and authentication.
- Create, update, delete and manage recruitment workspaces.
- Search candidates using unique email.
- Enroll and remove candidates from workspaces.
- Dashboard with candidate and assessment statistics.
- View detailed assessment reports, test failures and repository analysis.
2. Candidate Management
- Candidate registration and authentication.
- Join multiple recruitment workspaces.
- Dashboard showing upcoming and completed assessments.
- Enforce assessment start and end times.
- View final score, total tests, passed tests and failed tests.
- Prevent access to hidden test cases and internal evaluation details.
3. Assessment Creation (AI Pipeline)
- Configure assessments using GitHub repository URL, branch, difficulty and duration.
- Clone and validate the repository.
- Verify the original project builds and runs using Docker.
- Analyze repository architecture using Java AST parsing.
- Generate customized feature specifications using an LLM.
- Generate weighted HTTP test cases automatically.
- Track assessment generation progress.
4. Execution (Candidate IDE & Sandbox)
- Create an isolated copy of the candidate's repository.
- Provide hierarchical file explorer.
- Integrate Monaco code editor.
- Support file creation, deletion and renaming.
- Implement automatic code saving.
- Compile code and display errors.
- Execute applications inside isolated Docker containers.
- Stream application logs through a terminal.
- Start, monitor and stop execution containers.
5. Evaluation (Automated Grading)
- Submit assessment manually or automatically after timeout.
- Freeze candidate workspace after submission.
- Build and launch the submitted application in Docker.
- Execute hidden HTTP test cases sequentially.
- Validate HTTP status codes and JSON responses.
- Record execution time and individual test results.
- Calculate weighted assessment score.
- Generate detailed evaluation reports.
- Clean up temporary Docker resources.
- Provide separate recruiter and candidate reports.

Overall Component Flow
1. Recruiter Management

Creates workspace and enrolls candidates


2. Candidate Management

Registration, enrollment and scheduling


3. Assessment Creation

Git Clone → Docker Validation → AST → AI Feature → Test Generation


4. Execution

IDE → Code Changes → Compilation → Docker Runtime


5. Evaluation

Hidden API Tests → Assertions → Weighted Score → Reports


Core Database Entities
Component	Main entities
Recruiter	USER, WORKSPACE, WORKSPACE_CANDIDATE
Candidate	USER, WORKSPACE_CANDIDATE, ASSESSMENT
Assessment Creation	ASSESSMENT, REPOSITORY_ANALYSIS, FEATURE_SPECIFICATION, TEST_CASE
Execution	ASSESSMENT_WORKSPACE
Evaluation	SUBMISSION, EVALUATION_REPORT, TEST_RESULT

Core workflow: Recruiter creates an assessment → AI analyzes the repository and generates a feature → Candidate implements it in an isolated IDE → Evaluation engine runs hidden API tests → Recruiter receives a detailed report and candidate receives a safe score summary.