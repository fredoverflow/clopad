![clopad](https://i.imgur.com/wEU2NVd.png)

## Background

Do you struggle with setting up complicated Clojure development environments just to define your first function and evaluate some expressions?
Welcome to Clopad, a minimalistic Clojure code editor that will support you through your first steps!

## How do I compile clopad into an executable jar?
```
git clone https://github.com/fredoverflow/freditor
cd freditor
mvn install
cd ..
git clone https://github.com/fredoverflow/clopad
cd clopad
mvn package
```
The executable `clopad-x.y.z-SNAPSHOT-jar-with-dependencies.jar` will be located inside the `target` folder.
