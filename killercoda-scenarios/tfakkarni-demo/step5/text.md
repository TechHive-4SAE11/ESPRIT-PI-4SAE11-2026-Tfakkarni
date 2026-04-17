# Step 5 — Test the Alzheimer Risk Quiz API

The **ML Service** powers the Alzheimer risk quiz — a key feature of the landing page. It uses a machine learning model to predict risk based on patient health data.

## Check the ML service health

```bash
curl -s http://localhost:18085/actuator/health | python3 -m json.tool
```

## Submit a quiz through the API Gateway

The quiz endpoint accepts health parameters and returns a risk assessment. Let's test it with a **low-risk** profile:

```bash
curl -s -X POST http://localhost:9090/api/ml/quiz/predict \
  -H "Content-Type: application/json" \
  -d '{
    "age": 45,
    "gender": 1,
    "ethnicity": 0,
    "educationLevel": 3,
    "bmi": 24.5,
    "smoking": 0,
    "alcoholConsumption": 5.0,
    "physicalActivity": 8.0,
    "dietQuality": 7.0,
    "sleepQuality": 8.0,
    "familyHistoryAlzheimers": 0,
    "cardiovascularDisease": 0,
    "diabetes": 0,
    "depression": 0,
    "headInjury": 0,
    "hypertension": 0,
    "systolicBP": 120,
    "diastolicBP": 80,
    "cholesterolTotal": 190.0,
    "cholesterolLDL": 100.0,
    "cholesterolHDL": 55.0,
    "cholesterolTriglycerides": 140.0,
    "mmse": 28.0,
    "functionalAssessment": 9.0,
    "memoryComplaints": 0,
    "behavioralProblems": 0,
    "adl": 9.0,
    "confusion": 0,
    "disorientation": 0,
    "personalityChanges": 0,
    "difficultyCompletingTasks": 0,
    "forgetfulness": 0
  }' | python3 -m json.tool
```

Now test with a **high-risk** profile:

```bash
curl -s -X POST http://localhost:9090/api/ml/quiz/predict \
  -H "Content-Type: application/json" \
  -d '{
    "age": 82,
    "gender": 1,
    "ethnicity": 0,
    "educationLevel": 0,
    "bmi": 32.0,
    "smoking": 1,
    "alcoholConsumption": 15.0,
    "physicalActivity": 1.0,
    "dietQuality": 2.0,
    "sleepQuality": 3.0,
    "familyHistoryAlzheimers": 1,
    "cardiovascularDisease": 1,
    "diabetes": 1,
    "depression": 1,
    "headInjury": 1,
    "hypertension": 1,
    "systolicBP": 170,
    "diastolicBP": 100,
    "cholesterolTotal": 280.0,
    "cholesterolLDL": 180.0,
    "cholesterolHDL": 30.0,
    "cholesterolTriglycerides": 250.0,
    "mmse": 15.0,
    "functionalAssessment": 3.0,
    "memoryComplaints": 1,
    "behavioralProblems": 1,
    "adl": 3.0,
    "confusion": 1,
    "disorientation": 1,
    "personalityChanges": 1,
    "difficultyCompletingTasks": 1,
    "forgetfulness": 1
  }' | python3 -m json.tool
```

Compare the two results — notice how the risk score and recommendation differ based on the health parameters!

> **In the real app**, when a visitor on the landing page gets a mid-to-high risk score, a pop-up appears suggesting they book an appointment with a doctor.
