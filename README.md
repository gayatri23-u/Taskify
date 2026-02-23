# Taskify – Schedule Tracker Android App

Taskify is an Android application designed to help users manage daily tasks, monitor productivity through a monthly activity calendar, and maintain consistency with goal-oriented streaks. The app focuses on simplicity, usability, and effective habit tracking.

---

## Features

- Task creation for single or multiple days  
- Monthly activity calendar with completion indicators  
- Task completion tracking using tick and cross markers  
- Daily streak tracking based on full task completion  
- End-of-day reminders for pending tasks  
- Tabular task view for easy status updates  
- User profile with task statistics  
- Secure authentication using Firebase  
- Real-time cloud data storage with Firebase Firestore  
- Dark mode support  
- Settings management (profile, password, notifications, account deletion)

---

## Technology Stack

- Platform: Android  
- Language: Java  
- UI:XML, Material Design Components  
- Backend:Firebase  
  - Firebase Authentication  
  - Cloud Firestore  
- Image Handling: Glide  

---

## Application Modules / Screens

- Authentication (Login & Registration)  
- Dashboard with Activity Calendar  
- Add Task  
- View Tasks (Tabular Format)  
- Profile  
- Settings  

---

## System Overview

The application uses Firebase Authentication for secure user login.  
User-specific task data is stored in Cloud Firestore and synchronized in real time.  
The activity calendar reflects daily task completion status and updates streaks based on user performance.

---

## Setup Instructions

1. Clone the repository  
2. Open the project in Android Studio  
3. Connect your Firebase project  
4. Add the `google-services.json` file  
5. Build and run the application  

---

## Future Enhancements

- Weekly and monthly productivity analytics  
- Task categorization and prioritization  
- Home screen widgets  
- Data export functionality  

---

## Author

Gayatri Pawar
Entc engineer
Android Developer  
GitHub: gayatri23-u  

---

## License

This project is licensed under the MIT License.
