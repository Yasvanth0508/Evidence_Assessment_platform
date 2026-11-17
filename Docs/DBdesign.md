EVIDENCE – FINAL DATABASE DESIGN
Database: PostgreSQL
Total Tables: 12
Primary Key Strategy: UUID
Flexible Data Storage: JSONB

1. USERS
Purpose: Stores recruiters and candidates.
Attribute	Data Type	Constraints
id	UUID	PRIMARY KEY, DEFAULT gen_random_uuid()
name	VARCHAR(150)	NOT NULL
email	VARCHAR(255)	UNIQUE, NOT NULL
role	user_role	NOT NULL
password_hash	TEXT	NULL
auth_provider	VARCHAR(50)	NOT NULL
created_at	TIMESTAMPTZ	DEFAULT NOW()
updated_at	TIMESTAMPTZ	DEFAULT NOW()

ENUM: RECRUITER, CANDIDATE

Relationships:
- One USER can own multiple WORKSPACES.
- One USER can participate in multiple WORKSPACES through WORKSPACE_CANDIDATES.

2. WORKSPACES
Purpose: Represents a recruiter-owned repository workspace.
Attribute	Data Type	Constraints
id	UUID	PRIMARY KEY
recruiter_id	UUID	FOREIGN KEY → USERS.id, NOT NULL
name	VARCHAR(200)	NOT NULL
description	TEXT	NULL
github_url	TEXT	NOT NULL
repository_name	VARCHAR(255)	NULL
branch_name	VARCHAR(255)	NOT NULL
commit_sha	VARCHAR(100)	NULL
status	workspace_status	NOT NULL
created_at	TIMESTAMPTZ	DEFAULT NOW()
updated_at	TIMESTAMPTZ	DEFAULT NOW()

ENUM: CREATED, CLONING, ANALYZING, READY, FAILED

Relationships:
- Many WORKSPACES belong to one USER (recruiter).
- One WORKSPACE can have multiple WORKSPACE_CANDIDATES.

3. WORKSPACE_CANDIDATES
Purpose: Represents candidate membership in a workspace.
Attribute	Data Type	Constraints
workspace_id	UUID	PRIMARY KEY, FOREIGN KEY → WORKSPACES.id
candidate_id	UUID	PRIMARY KEY, FOREIGN KEY → USERS.id
status	VARCHAR(30)	NOT NULL
joined_at	TIMESTAMPTZ	DEFAULT NOW()

Primary Key: (workspace_id, candidate_id)

Status values: INVITED, JOINED, IN_PROGRESS, COMPLETED, WITHDRAWN

Relationships:
- Many WORKSPACE_CANDIDATES belong to one WORKSPACE.
- Many WORKSPACE_CANDIDATES belong to one USER (candidate).
- One WORKSPACE_CANDIDATE can have at most one ASSESSMENT.

4. ASSESSMENTS
Purpose: Represents an individual assessment assigned to a candidate.
Attribute	Data Type	Constraints
id	UUID	PRIMARY KEY
workspace_id	UUID	COMPOSITE FOREIGN KEY → WORKSPACE_CANDIDATES.workspace_id
candidate_id	UUID	COMPOSITE FOREIGN KEY → WORKSPACE_CANDIDATES.candidate_id
exam_code	VARCHAR(30)	UNIQUE, NOT NULL
difficulty	difficulty_level	NOT NULL
duration_minutes	INTEGER	NOT NULL, CHECK > 0
scheduled_start_at	TIMESTAMPTZ	NULL
scheduled_end_at	TIMESTAMPTZ	NULL
status	assessment_status	NOT NULL
created_at	TIMESTAMPTZ	DEFAULT NOW()
updated_at	TIMESTAMPTZ	DEFAULT NOW()

ENUM values:
- difficulty_level: EASY, MEDIUM, HARD
- assessment_status: DRAFT, PUBLISHED, IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED
Constraints:
- FOREIGN KEY (workspace_id, candidate_id) → WORKSPACE_CANDIDATES (workspace_id, candidate_id).
- UNIQUE (workspace_id, candidate_id).
- CHECK duration_minutes > 0.
- CHECK scheduled_end_at > scheduled_start_at when both are provided.
Relationships:
- One WORKSPACE_CANDIDATE has at most one ASSESSMENT.
- One ASSESSMENT can have one REPOSITORY_ANALYSIS.
- One ASSESSMENT can have one FEATURE_SPECIFICATION.
- One ASSESSMENT can have one ASSESSMENT_WORKSPACE.
- One ASSESSMENT can have one SUBMISSION.

5. REPOSITORY_ANALYSIS
Purpose: Stores static repository analysis results generated using JavaParser.
Attribute	Data Type	Constraints
assessment_id	UUID	PRIMARY KEY, FOREIGN KEY → ASSESSMENTS.id
project_structure	JSONB	NOT NULL
source_code_structure	JSONB	NOT NULL
content_details	JSONB	NOT NULL
parser_version	VARCHAR(50)	NULL
status	analysis_status	NOT NULL
created_at	TIMESTAMPTZ	DEFAULT NOW()
completed_at	TIMESTAMPTZ	NULL

ENUM: PENDING, PROCESSING, COMPLETED, FAILED

Relationships:
- One REPOSITORY_ANALYSIS belongs to one ASSESSMENT.
- One ASSESSMENT has at most one REPOSITORY_ANALYSIS.

6. ASSESSMENT_WORKSPACE
Purpose: Stores filesystem information for the candidate's execution workspace.
Attribute	Data Type	Constraints
assessment_id	UUID	PRIMARY KEY, FOREIGN KEY → ASSESSMENTS.id
original_repository_path	TEXT	NOT NULL
candidate_workspace_path	TEXT	NULL
created_at	TIMESTAMPTZ	DEFAULT NOW()
updated_at	TIMESTAMPTZ	DEFAULT NOW()

Relationships:
- One ASSESSMENT has at most one ASSESSMENT_WORKSPACE.
- One ASSESSMENT_WORKSPACE belongs to one ASSESSMENT.

7. FEATURE_SPECIFICATION
Purpose: Stores the AI-generated API feature requirements.
Attribute	Data Type	Constraints
assessment_id	UUID	PRIMARY KEY, FOREIGN KEY → ASSESSMENTS.id
feature_name	VARCHAR(255)	NOT NULL
description	TEXT	NOT NULL
requirements	JSONB	NOT NULL
request_specification	JSONB	NULL
response_specification	JSONB	NULL
constraints	JSONB	NULL
created_at	TIMESTAMPTZ	DEFAULT NOW()
updated_at	TIMESTAMPTZ	DEFAULT NOW()

Relationships:
- One FEATURE_SPECIFICATION belongs to one ASSESSMENT.
- One FEATURE_SPECIFICATION can have multiple TEST_CASES.

8. TEST_CASES
Purpose: Stores AI-generated hidden HTTP test cases.
Attribute	Data Type	Constraints
id	UUID	PRIMARY KEY
assessment_id	UUID	FOREIGN KEY → FEATURE_SPECIFICATION.assessment_id
test_case_number	INTEGER	NOT NULL
test_type	test_type	NOT NULL
http_method	VARCHAR(20)	NOT NULL
endpoint	VARCHAR(500)	NOT NULL
request_data	JSONB	NULL
expected_status_code	INTEGER	NOT NULL
expected_response	JSONB	NULL
assertions	JSONB	NOT NULL
weight	DECIMAL(5,2)	NOT NULL, CHECK >= 0
is_active	BOOLEAN	DEFAULT TRUE
created_at	TIMESTAMPTZ	DEFAULT NOW()

ENUM: POSITIVE, NEGATIVE, BOUNDARY, EDGE_CASE, REGRESSION

Constraints:
- UNIQUE (assessment_id, test_case_number).
- CHECK weight >= 0.
- CHECK test_case_number > 0.
- CHECK expected_status_code BETWEEN 100 AND 599.
- HTTP method must be GET, POST, PUT, PATCH or DELETE.
Relationships:
- Multiple TEST_CASES belong to one FEATURE_SPECIFICATION.
- One TEST_CASE can have multiple TEST_RESULTS across executions.

9. SUBMISSIONS
Purpose: Stores the candidate's single final code submission.
Attribute	Data Type	Constraints
id	UUID	PRIMARY KEY
assessment_id	UUID	FOREIGN KEY → ASSESSMENTS.id, UNIQUE
source_snapshot_path	TEXT	NOT NULL
submitted_at	TIMESTAMPTZ	DEFAULT NOW()
time_taken_seconds	BIGINT	NOT NULL, CHECK >= 0
status	submission_status	NOT NULL

ENUM: SUBMITTED, PROCESSING, COMPLETED, FAILED

Relationships:
- One ASSESSMENT can have at most one SUBMISSION.
- One SUBMISSION can have at most one EXECUTION.

10. EXECUTIONS
Purpose: Stores Docker execution information.
Attribute	Data Type	Constraints
id	UUID	PRIMARY KEY
submission_id	UUID	FOREIGN KEY → SUBMISSIONS.id, UNIQUE
container_id	VARCHAR(255)	NULL
status	execution_status	NOT NULL
build_status	build_status	NOT NULL
application_status	application_status	NOT NULL
started_at	TIMESTAMPTZ	NULL
completed_at	TIMESTAMPTZ	NULL
execution_time_seconds	BIGINT	CHECK >= 0
build_log_path	TEXT	NULL
application_log_path	TEXT	NULL

ENUM values:
- execution_status: PENDING, CREATING_CONTAINER, BUILDING, STARTING, RUNNING_TESTS, COMPLETED, FAILED, TIMED_OUT
- build_status: PENDING, SUCCESS, FAILED
- application_status: PENDING, STARTED, FAILED
Relationships:
- One SUBMISSION can have at most one EXECUTION.
- One EXECUTION can have multiple TEST_RESULTS.
- One EXECUTION can have one EVALUATION_REPORT.

11. TEST_RESULTS
Purpose: Stores the results of individual HTTP test executions.
Attribute	Data Type	Constraints
execution_id	UUID	PRIMARY KEY, FOREIGN KEY → EXECUTIONS.id
test_case_id	UUID	PRIMARY KEY, FOREIGN KEY → TEST_CASES.id
status	test_result_status	NOT NULL
actual_status_code	INTEGER	NULL
actual_response	JSONB	NULL
failure_reason	TEXT	NULL
execution_time_ms	BIGINT	CHECK >= 0

Composite Primary Key: (execution_id, test_case_id)

ENUM: PASS, FAIL, ERROR, SKIPPED

Relationships:
- Multiple TEST_RESULTS belong to one EXECUTION.
- Multiple TEST_RESULTS can reference one TEST_CASE.
- Each result belongs to exactly one execution and one test case.
- The execution and test case must belong to the same assessment.

12. EVALUATION_REPORTS
Purpose: Stores the final evaluation report for a candidate.
Attribute	Data Type	Constraints
execution_id	UUID	PRIMARY KEY, FOREIGN KEY → EXECUTIONS.id
score	DECIMAL(5,2)	NOT NULL, CHECK 0–100
total_tests	INTEGER	NOT NULL, CHECK >= 0
passed_tests	INTEGER	NOT NULL, CHECK >= 0
failed_tests	INTEGER	NOT NULL, CHECK >= 0
status	evaluation_status	NOT NULL
evaluated_at	TIMESTAMPTZ	NULL

ENUM: PENDING, COMPLETED, FAILED

Additional constraints:
- passed_tests + failed_tests <= total_tests.
- score BETWEEN 0 AND 100.
Relationships:
- One EXECUTION can have at most one EVALUATION_REPORT.
- One EVALUATION_REPORT belongs to one EXECUTION.

13. Global relationship summary
Parent table	Child table	Relationship
USERS	WORKSPACES	1:N
USERS	WORKSPACE_CANDIDATES	1:N
WORKSPACES	WORKSPACE_CANDIDATES	1:N
WORKSPACE_CANDIDATES	ASSESSMENTS	1:0..1
ASSESSMENTS	REPOSITORY_ANALYSIS	1:0..1
ASSESSMENTS	ASSESSMENT_WORKSPACE	1:0..1
ASSESSMENTS	FEATURE_SPECIFICATION	1:0..1
FEATURE_SPECIFICATION	TEST_CASES	1:N
ASSESSMENTS	SUBMISSIONS	1:0..1
SUBMISSIONS	EXECUTIONS	1:0..1
EXECUTIONS	TEST_RESULTS	1:N
TEST_CASES	TEST_RESULTS	1:N
EXECUTIONS	EVALUATION_REPORTS	1:0..1


14. Important global constraints
1. Every email must be unique.
2. Every workspace must belong to a recruiter.
3. A candidate can join a workspace only once.
4. A candidate can have only one assessment per workspace.
5. Every examination code must be unique.
6. Assessment duration must be greater than zero.
7. Scheduled end time must be later than scheduled start time when both exist.
8. Every assessment can have only one final submission.
9. Every submission can have only one execution.
10. Every test case number must be unique within an assessment.
11. Test weights cannot be negative.
12. Test results must belong to the same assessment as their execution.
13. Evaluation scores must be between 0 and 100.
14. Passed and failed test counts cannot exceed total tests.
15. Hidden test definitions must not be accessible through candidate-facing operations.
Design summary: 12 tables, UUID-based identifiers, composite keys for workspace membership and test results, JSONB for AI-generated structures, and individual assessment, submission and execution records.