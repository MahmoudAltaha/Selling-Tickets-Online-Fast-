# Selling Tickets Online (Fast)

Template for the concurrent programming project 2022.  🚀

We highly recommend that you take a close look at the entire template and think about your solution *before* you write any code. It is a good idea to start with the `Database` and the `Mailbox` classes. For the `Mailbox` class we also provide unit tests and additional hints for the implementation.


## Structure

This project is structured as follows:

- `src/main/java/com/pseuco/np22/`: Java source code of the project.
    - `request`: Infrastructure for working with requests.
    - `rocket`: Your implementation should go here.
    - `slug`: A slow and sequential reference implementation.
- `src/test`: Unit tests.


## Gradle

We use [Gradle](https://gradle.org/) to build the project.

To build the Javadoc run:
```bash
./gradlew javaDoc
```
Afterwards you find the documentation in `build/docs`.


To build a `cli.jar`-File for your project run:
```bash
./gradlew jar
```
You find the compiled `.jar`-File in `out`.


## Testing

We provide an interactive web frontend for testing.


## Integrated Development Environment

We recommend you use a proper *Integrated Development Environment* (IDE) for this project.
A good choice you should already be familiar with from *Programming* 2 is [Eclipse](https://www.eclipse.org/).
Another good open source IDE is [VS Code](https://code.visualstudio.com/).
While Eclipse is more focused on Java and provides a better experience when it comes to Java programming, VS Code is more universal and might be worth using as a general editor during your studies for all kinds of tasks like writing your bachelor's thesis.
Which IDE or editor you use is up to you.
However, we only provide help for Eclipse and VS Code.
In case you use something else, do not expect help.


### Visual Studio Code

In case you decide to use VS Code, we recommend installing the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) and the [Gradle Extension Pack](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-gradle-extension-pack).
