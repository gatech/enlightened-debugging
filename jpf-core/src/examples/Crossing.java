/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */




import gov.nasa.jpf.vm.Verify;

enum Side { LEFT, RIGHT }

class Bridge {

  Side torchSide;
  Person[] onBridge = new Person[2];
  int numOnBridge = 0;

  public Bridge (Side torchSide){
    this.torchSide = torchSide;
  }

  void moveTorch(){
    if (torchSide == Side.LEFT){
      torchSide = Side.RIGHT;
    } else {
      torchSide = Side.LEFT;
    }
  }
  
  public int cross() {
    int time = 0;
    moveTorch();

    if (numOnBridge == 1) {
      onBridge[0].side = torchSide;
      time = onBridge[0].timeToCross;




    } else {
      onBridge[0].side = torchSide;
      onBridge[1].side = torchSide;

      if (onBridge[0].timeToCross > onBridge[1].timeToCross) {
        time = onBridge[0].timeToCross;
      } else {
        time = onBridge[1].timeToCross;
      }





    }

    return time;
  }

  public void clearBridge() {
    numOnBridge = 0;
    onBridge[0] = null;
    onBridge[1] = null;
  }
  
  boolean isFull(){
    return numOnBridge == 2;
  }
  
  boolean arePersonsOnBridge(){
    return numOnBridge > 0;
  }
  
  boolean isPersonOnBridge (Person p){
    return (p == onBridge[0] || p == onBridge[1]);
  }

  void putPersonOnBridge (Person p){
    onBridge[numOnBridge++] = p;
  }  
}

class Person {
  String name;
  Side side;
  int timeToCross;

  public Person(String name, int timeToCross) {
    this.timeToCross = timeToCross;
    this.name = name;
  }

  public void tryToMove (Bridge bridge) {
    if (side == bridge.torchSide) {
      if (!Verify.getBoolean()) {
        if (! (bridge.isFull() || bridge.isPersonOnBridge(this))){
          bridge.putPersonOnBridge(this);
        }
      }
    }
  }
  
  @Override
  public String toString(){
    return name;
  }
}

public class Crossing {

  Bridge bridge;
  Person[] persons;
  int elapsedTime;
  Side initialSide;
  
  public Crossing (Person[] persons, Side initialSide){
    this.persons = persons;
    for (Person p : persons){
      p.side = initialSide;
    }
    
    this.bridge = new Bridge( initialSide);
    this.initialSide = initialSide;
  }
  
  boolean haveAllPersonsCrossed (){
    for (Person p : persons){
      if (p.side == initialSide){
        return false;
      }
    }
    
    return true;
  }
  
  void solve (){
    printPersons();
    System.out.println();
    printCrossingState();

      
    while (!haveAllPersonsCrossed()){
      for (Person p : persons){
        p.tryToMove(bridge);
      }

      if (bridge.arePersonsOnBridge()) {
        elapsedTime += bridge.cross();




        bridge.clearBridge();
      }
      
      printCrossingState();      
    }
    
    
    System.out.println("total time = " + elapsedTime);
    

    Verify.printPathOutput("done");
    Verify.storeTraceAndTerminateIf(elapsedTime == 17, null, null);

  }
  
  String personsOnSide (Side side){
    int n=0;
    StringBuilder sb = new StringBuilder();
    for (Person p : persons){
      if (p.side == side){
        if (n++ > 0){
          sb.append(',');
        }
        sb.append( p);
      }
    }
    return sb.toString();
  }
  
  String torchSymbol (Side side){
    if (bridge.torchSide == side){
      return "*";
    } else {
      return " ";
    }
  }
  
  void printPersons(){
    for (Person p : persons){
      System.out.printf("%10s needs %2d min to cross\n", p.name, p.timeToCross);
    }
  }
  
  void printCrossingState (){
    System.out.printf("%20s %s====%s %-20s : elapsed time=%d\n",
            personsOnSide(Side.LEFT), torchSymbol(Side.LEFT),
            torchSymbol(Side.RIGHT), personsOnSide(Side.RIGHT), elapsedTime);
  }
  
  public static void main(String[] args) {
    Person[] persons = {
      new Person("Bill", 1),
      new Person("Xoe", 2),
      new Person("Sue", 5),
      new Person("Joe", 10)
    };

    Crossing crossing = new Crossing( persons, Side.LEFT);
    crossing.solve();
  }
}
