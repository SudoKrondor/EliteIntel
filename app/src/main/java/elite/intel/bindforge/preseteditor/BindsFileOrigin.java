package elite.intel.bindforge.preseteditor;

/** Where a binds file lives. */
public enum BindsFileOrigin {

    /** The player's bindings folder. */
    USER,

    /** An installation's {@code ControlSchemes} folder, as Frontier shipped it. Read-only to BindForge. */
    FACTORY
}
