Attendify ✨
A modern, real-time attendance management system for Android.

Attendify is a feature-rich Android application designed to digitize and simplify the process of tracking class attendance. It provides an intuitive, real-time solution that eliminates the need for manual record-keeping, making attendance data instantly accessible and manageable for both students and instructors.

🎯 Project Goal
Our goal is to provide an efficient, reliable, and user-friendly platform that bridges the communication gap between students and faculty regarding attendance. By leveraging modern mobile technology and cloud services, Attendify aims to foster a more accountable and transparent academic environment.

🌟 Features
For Students
Secure Sign-Up/Login: Easy and secure account creation and access.

Course Enrollment: Browse and enroll in available courses.

Real-time Attendance View: Check your attendance status for each class as soon as it's marked.

Attendance History: Access a detailed history of your attendance records for all enrolled courses.

Profile Management: View and manage your personal profile information.

For Teachers/Admins
Secure Sign-Up/Login: Dedicated portal with administrative privileges.

Course Management: Create, update, and delete courses for different classes or semesters.

Student Management: View a list of students enrolled in each course.

Live Attendance Marking: Mark students as present, absent, or late in real-time during a class session.

Dashboard & Analytics: A comprehensive dashboard with visual statistics on class attendance rates.

Generate Reports: Export attendance data for administrative purposes.

🛠️ Technology & Architecture
Attendify is built using a modern, scalable, and maintainable technology stack.

Technology Stack
Platform: Android

Language: Kotlin - For modern, concise, and safe code.

UI Toolkit: Android Jetpack (likely using XML with View Binding or Jetpack Compose).

Backend: Google Firebase

Cloud Firestore: NoSQL database for real-time data synchronization of users, courses, and attendance records.

Firebase Authentication: For secure handling of user sign-up, login, and session management.

Build Tool: Gradle - For managing dependencies and automating the build process.

Architectural Pattern
The application follows the Model-View-ViewModel (MVVM) architectural pattern.

Model: Represents the data and business logic (e.g., fetching data from Firestore).

View: The UI layer (Activities/Fragments) that observes data changes.

ViewModel: Acts as a bridge between the Model and the View, holding and processing UI-related data in a lifecycle-conscious way.

This separation of concerns makes the app easier to test, debug, and scale.

🚀 Getting Started
Follow these instructions to get a local copy of the project up and running on your machine for development and testing purposes.

Prerequisites
Android Studio (latest version recommended)

A Google account to create a Firebase project

Installation
Clone the repository:

git clone https://github.com/Coder69-ops/Attendify.git

Open in Android Studio:

Launch Android Studio.

Select File > Open and choose the cloned project directory.

Wait for Gradle to sync all project dependencies.

Connect to Firebase:

Visit the Firebase Console.

Create a new project.

Inside your project, add a new Android application. You will be prompted for a package name. Use the applicationId found in your app/build.gradle.kts file.

Follow the setup steps to enable Authentication (with the Email/Password provider) and Cloud Firestore.

Download the google-services.json configuration file.

Switch to the "Project" view in Android Studio's project explorer and place the google-services.json file inside the app/ directory.

Build and Run:

Build the project from Build > Make Project.

Run the application on an Android emulator or a physical device via Run > Run 'app'.

🤝 How to Contribute
Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

Fork the Project

Create your Feature Branch (git checkout -b feature/AmazingFeature)

Commit your Changes (git commit -m 'Add some AmazingFeature')

Push to the Branch (git push origin feature/AmazingFeature)

Open a Pull Request

Please make sure to update tests as appropriate.

📜 License
Distributed under the MIT License. See LICENSE for more information.

📬 Contact
 Ovejit Das - oveisawesome@gmail.com - https://www.linkedin.com/in/ovejit-das-826987354/

Project Link: https://github.com/Coder69-ops/Attendify
