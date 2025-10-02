/*
 * Copyright 2025 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.app.keymaster.command;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Component
@Command(group = "Built-In Commands")
public class SortedHelpCommand
{
  private final CommandCatalog commandCatalog;

  public SortedHelpCommand(@Lazy CommandCatalog commandCatalog)
  {
    this.commandCatalog = commandCatalog;
  }

  @Command(command = "help",
           description = "Display help about available commands")
  public String execute()
  {
    StringBuilder sb = new StringBuilder();

    Map<String, List<CommandRegistration>> commandsByGroup =
      commandCatalog.getRegistrations().values().stream()
        .collect(Collectors.groupingBy(
          CommandRegistration :: getGroup,
          TreeMap :: new,
          Collectors.toList()
        ));

    sb.append("AVAILABLE COMMANDS\n\n");

    commandsByGroup.forEach((group, registrations) ->
    {
      sb.append(group).append("\n");

      registrations.stream()
        .sorted(Comparator.comparing(reg -> reg.getCommand()))
        .forEach(reg ->
        {
          String command = String.format(
            "       %s: %s", reg.getCommand(), reg.getDescription());
          sb.append(command).append("\n");
        });
      sb.append("\n");
    });

    return sb.toString();
  }

}
