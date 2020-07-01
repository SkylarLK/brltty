/*
 * libbrlapi - A library providing access to braille terminals for applications.
 *
 * Copyright (C) 2006-2020 by
 *   Samuel Thibault <Samuel.Thibault@ens-lyon.org>
 *   Sébastien Hinderer <Sebastien.Hinderer@ens-lyon.org>
 *
 * libbrlapi comes with ABSOLUTELY NO WARRANTY.
 *
 * This is free software, placed under the terms of the
 * GNU Lesser General Public License, as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any
 * later version. Please see the file LICENSE-LGPL for details.
 *
 * Web Page: http://brltty.app/
 *
 * This software is maintained by Dave Mielke <dave@mielke.cc>.
 */

package org.a11y.brlapi;
import org.a11y.brlapi.programs.*;
import org.a11y.brlapi.clients.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class Programs extends ProgramComponent {
  private Programs () {
  }

  private final static KeywordMap<Class<? extends Program>> programs = new KeywordMap<>();

  private static void addProgram (Class<? extends Program> type) {
    String name = type.getSimpleName();

    if (Client.class.isAssignableFrom(type)) {
      name = name.replaceAll("Client$", "");
    } else {
      name = name.replaceAll("Program$", "");
    }

    programs.put(toName(wordify(name)), type);
  }

  static {
    addProgram(BoundCommandsClient.class);
    addProgram(ComputerBrailleClient.class);
    addProgram(DriverKeysClient.class);
    addProgram(EchoClient.class);
    addProgram(ListParametersClient.class);
    addProgram(PauseClient.class);
    addProgram(SetParameterClient.class);
    addProgram(VersionProgram.class);
  }

  private static class MainProgram extends Program {
    public MainProgram (String... arguments) {
      super(arguments);
      addRequiredParameters("program/client");
      addOptionalParameters("arguments");
    }

    @Override
    protected final void extendUsageSummary (StringBuilder usage) {
      if (programs.isEmpty()) {
        usage.append("No programs or clients have been defined.");
      } else {
        usage.append("These programs and clients have been defined:");

        for (String name : programs.getKeywords()) {
          usage.append("\n  ");
          usage.append(name);
        }
      }
    }

    private String programName = null;
    private Class<? extends Program> programType = null;
    private String[] programArguments = null;

    @Override
    protected final void processParameters (String[] parameters)
              throws SyntaxException
    {
      int count = parameters.length;

      if (count == 0) {
        throw new SyntaxException("missing program/client name");
      }

      programName = parameters[0];
      programType = programs.get(programName);

      if (programType == null) {
        throw new SyntaxException("unknown program/client: %s", programName);
      }

      count -= 1;
      programArguments = new String[count];
      System.arraycopy(parameters, 1, programArguments, 0, count);
    }

    @Override
    protected final void runProgram () throws ProgramException {
      String term = Client.class.isAssignableFrom(programType)? "client": "program";
      Program program = null;

      try {
        Constructor<? extends Program> constructor = programType.getConstructor(
          programArguments.getClass()
        );

        program = (Program)constructor.newInstance((Object)programArguments);
      } catch (NoSuchMethodException exception) {
        throw new ProgramException(
          "%s constructor not found: %s", term, programName
        );
      } catch (InstantiationException exception) {
        throw new ProgramException(
          "%s instantiation failed: %s: %s", term, programName, exception.getMessage()
        );
      } catch (IllegalAccessException exception) {
        throw new ProgramException(
          "%s object access denied: %s: %s", term, programName, exception.getMessage()
        );
      } catch (InvocationTargetException exception) {
        throw new ProgramException(
          "%s construction failed: %s: %s", term, programName, exception.getCause().getMessage()
        );
      }

      program.run();
    }
  }

  public static void main (String arguments[]) {
    new MainProgram(arguments).run();
  }
}
