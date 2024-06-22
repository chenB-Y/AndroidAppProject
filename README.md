# Online Shoplist Application
## Introduction
The Online Shoplist Application is designed to help users create, manage, and share shopping lists. Users can add items to their lists, and mark them as purchased. The application also allows users to search for recipes using the Ninja API, making meal planning easier and more efficient.

![image](https://github.com/chenB-Y/AndroidAppProject/assets/129218828/4ba54224-2e5a-40cd-85b3-5535d42cc083)

## Features
- User Authentication: Users can register and log in to their accounts.
- Group Management: Users can be part of multiple groups and share shopping lists within the group.
- Product Management: Add, edit, and delete products in the shopping list.
- Location-based Product Listing: Products are shown on a map based on their location.
- Recipe Search: Search for recipes using the Ninja API and display them in a list.
- Firebase Integration: Store and retrieve user data, group information, and product details using Firebase Firestore and Firebase Storage.
## Technologies Used
- Android Studio: For Android app development.
- Firebase Firestore: For real-time database and user authentication.
- Firebase Storage: For storing product images.
- Retrofit: For API calls to the Ninja API.
- RecyclerView: For displaying lists of products and recipes.
- Google Maps: For displaying product locations on a map.
- Material Design: For creating a modern and user-friendly UI.
## Installation and Setup
1. Clone the Repository <br>
   git clone https://github.com/yourusername/onlineshoplist.git
2. Open the Project in Android Studio

3. Configure Firebase
- Go to the Firebase Console and create a new project.
- Add your Android app to the project and download the google-services.json file.
- Place the google-services.json file in the app directory of your project.
- Enable Firestore, Firebase Authentication, and Firebase Storage in the Firebase Console.
4. Add API Key for Ninja API
- Open NinjaApiService.java and replace YOUR_NINJA_API_KEY with your actual Ninja API key.

5. Build and Run the Project

## Usage

## Authentication
- Register: Users can register by providing their email and password.
- Login: Registered users can log in using their email and password.
## Group Management
- Create Group: Users can create new groups.
- Join Group: Users can join existing groups using an invitation.
## Product Management
- Add Product: Users can add new products to the shopping list.
- Edit Product: Users can edit existing products.
- Delete Product: Users can delete products from the list.
- View Product Locations: Products are displayed on a map based on their location.
##Recipe Search
- Search Recipes: Users can search for recipes by entering a food name.
- View Recipes: Recipes are displayed in a list with details.
## Project Structure
- Adapters: Contains all RecyclerView adapters.
- Models: Contains data models for Product, Group, etc.
- Fragments: Contains all UI fragments such as RecipesFragment, UserProductsFragment.
- Services: Contains service classes for API calls and Firebase operations.
- Utils: Contains utility classes and methods.
## Contributing
Contributions are welcome! Please create a pull request with detailed information about the changes.
