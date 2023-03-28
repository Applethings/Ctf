package ga.pmc.ctf;

import org.bukkit.command.CommandExecutor;

public class CtfSubCommand {

    public String name;
    public boolean admin;
    public CommandExecutor executor;

    public CtfSubCommand(String n, boolean a, CommandExecutor e) {
        name = n;
        admin = a;
        executor = e;
    }

}
