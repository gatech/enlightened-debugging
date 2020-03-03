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


package gov.nasa.jpf.util.script;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.util.StateExtensionClient;
import gov.nasa.jpf.util.StateExtensionListener;
import gov.nasa.jpf.vm.ChoiceGenerator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;


public abstract class ScriptEnvironment<CG extends ChoiceGenerator<?>> 
         implements StateExtensionClient<ScriptEnvironment<CG>.ActiveSnapshot> {

  static final String DEFAULT = "default";


  static class ActiveSequence implements Cloneable {
    String stateName;
    Section section;
    SequenceInterpreter intrp;

    public ActiveSequence (String stateName, Section section, SequenceInterpreter intrp) {
      this.stateName = stateName;
      this.section = section;
      this.intrp = intrp;
    }

    @Override
	public Object clone() {
      try {
        ActiveSequence as = (ActiveSequence) super.clone();
        as.intrp = (SequenceInterpreter) intrp.clone();
        return as;
      } catch (CloneNotSupportedException nonsense) {
        return null; 
      }
    }

    public boolean isDone() {
      return intrp.isDone();
    }
  }


  class ActiveSnapshot implements Cloneable {
    ActiveSequence[] actives;

    ActiveSnapshot () {
      actives = new ActiveSequence[0];
    }

    ActiveSnapshot (ActiveSequence[] as) {
      actives = as;
    }

    public ActiveSequence get (String stateName) {
      for (ActiveSequence as : actives) {
        if (as.stateName.equals(stateName)) {
          return as;
        }
      }
      return null;
    }

    @Override
	public Object clone() {
      try {
        ActiveSnapshot ss = (ActiveSnapshot)super.clone();
        for (int i=0; i<actives.length; i++) {
          ActiveSequence as = actives[i];
          ss.actives[i] = (ActiveSequence)as.clone();
        }
        return ss;
      } catch (CloneNotSupportedException nonsense) {
        return null; 
      }
    }

    ActiveSnapshot advance (String[] activeStates, BitSet isReEntered) {
      ActiveSequence[] newActives = new ActiveSequence[activeStates.length];


      for (int i=0; i<activeStates.length; i++) {
        String sn = activeStates[i];
        for (ActiveSequence as : actives) {
          if (as.stateName.equals(sn) ) {


            newActives[i] = (ActiveSequence)as.clone();
          }
        }
      }


      int skipped = 0;
      nextActive:
      for (int i=0; i<activeStates.length; i++) {
        if (newActives[i] == null) {

          Section sec = getSection(activeStates[i]);
          if (sec != null) {


            for (int j=0; j<newActives.length; j++) {
              if (newActives[j] != null && newActives[j].section == sec) {
                skipped++;
                continue nextActive;
              }
            }



            for (int j=0; j<actives.length; j++) {

              if (actives[j].section == sec) {
                ActiveSequence as = new ActiveSequence(activeStates[i], sec, actives[j].intrp);
                newActives[i] = as;
                continue nextActive;
              }
            }


            ActiveSequence as = new ActiveSequence(activeStates[i], sec,
                                                   new SequenceInterpreter(sec));
            newActives[i] = as;

          } else { 
            skipped++;
          }
        }
      }


      if (skipped > 0) {
        int n = activeStates.length - skipped;
        ActiveSequence[] na = new ActiveSequence[n];
        for (int i=0, j=0; j<n; i++) {
          if (newActives[i] != null) {
            na[j++] = newActives[i];
          }
        }
        newActives = na;
      }

      return new ActiveSnapshot(newActives);
    }
  }



  String scriptName;
  Reader scriptReader;
  Script script;
  ActiveSnapshot cur;

  HashMap<String,Section> sections = new HashMap<String,Section>();
  Section defaultSection;


  public ScriptEnvironment (String fname) throws FileNotFoundException {
    this( fname, new FileReader(fname));
  }

  public ScriptEnvironment (String name, Reader r) {
    this.scriptName = name;
    this.scriptReader = r;
  }

  public void parseScript () throws ESParser.Exception {
    ESParser parser= new ESParser(scriptName, scriptReader);
    script = parser.parse();

    initSections();

    cur = new ActiveSnapshot();
  }

  void initSections() {
    Section defSec = new Section(script, DEFAULT);

    for (ScriptElement e : script) {

      if (e instanceof Section) {
        Section sec = (Section)e;
        List<String> secIds = sec.getIds();
        if (secIds.size() > 0) {
          for (String id : secIds) {
            sections.put(id, (Section)sec.clone()); 
          }
        } else {
          sections.put(secIds.get(0), sec);
        }
      } else { 
        defSec.add(e.clone());
      }
    }

    if (defSec.getNumberOfChildren() > 0) {
      defaultSection = defSec;
    }
  }

  Section getSection (String id) {
    Section sec = null;

    while (id != null) {
      sec = sections.get(id);
      if (sec != null) {
        return sec;
      }

      int idx = id.lastIndexOf('.');
      if (idx > 0) {
        id = id.substring(0, idx); 
      } else {
        id = null;
      }
    }

    return defaultSection;
  }

  void addExpandedEvent(ArrayList<Event> events, Event se) {
    for (Event e : se.expand()) {
      if (!events.contains(e)) {
        events.add(e);
      }
    }
  }

  static final String[] ACTIVE_DEFAULT = { DEFAULT };

  public CG getNext (String id) {
    return getNext(id, ACTIVE_DEFAULT, null);
  }

  public CG getNext (String id, String[] activeStates) {
    return getNext(id, activeStates, null);
  }


  public CG getNext (String id, String[] activeStates, BitSet isReEntered) {

    cur = cur.advance(activeStates, isReEntered);

    ArrayList<Event> events = new ArrayList<Event>(cur.actives.length);
    for (ActiveSequence as : cur.actives) {

      while (true) {
        ScriptElement se = as.intrp.getNext();
        if (se != null) {
          if (se instanceof Event) {
            addExpandedEvent(events, (Event)se);
            break;
          } else if (se instanceof Alternative) {
            for (ScriptElement ase : (Alternative)se) {
              if (ase instanceof Event) {
                addExpandedEvent(events, (Event)ase);
              }
            }
            break;
          } else {

          }
        } else {
          break; 
        }
      }
    }

    return createCGFromEvents(id, events);
  }

  protected abstract CG createCGFromEvents(String id, List<Event> events);


  @Override
  public ActiveSnapshot getStateExtension() {
    return cur;
  }

  @Override
  public void restore(ActiveSnapshot stateExtension) {
    cur = stateExtension;
  }

  @Override
  public void registerListener(JPF jpf) {
    StateExtensionListener<ActiveSnapshot> sel = new StateExtensionListener(this);
    jpf.addSearchListener(sel);
  }

}
