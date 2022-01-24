package cool.parser.AST;

import cool.compiler.Compiler;
import cool.parser.CoolParser;
import cool.structures.ActualClassSymbol;
import cool.structures.ClassSymbol;
import cool.structures.IdSymbol;
import cool.structures.MethodSymbol;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ASTCodeGenPassVisitor extends ASTDefaultVisitor<ST> {
    // Numărul de bytes dintr-un cuvânt.
    private static final int MIPS_WORD_SIZE = 4;

    // Numărul de cuvinte din headerul unui obiect (tag, size, vmtable).
    private static final int MIPS_PROT_OBJ_HEADER_NUM_WORDS = 3;

    // Numărul de cuvinte ce se află între $fp și prima locație de var. locală.
    private static final int MIPS_NUM_WORDS_UNTIL_FIRST_LOCAL_FROM_FP = 1;

    // Numărul de cuvinte ce se află între $fp și primul parametru formal ($ra, $s0, $fp).
    private static final int MIPS_NUM_WORDS_UNTIL_FIRST_FORMAL_FROM_FP = 3;

    private static final STGroupFile templates = new STGroupFile("cgen.stg");

    // Număr folosit pentru numerotarea label-urilor (eg: dispatchX, thenBranchX)
    private int uniqCounter = 0;

    // Fișierul sursă în care este definită clasa pentru care se generează cod la un moment dat.
    private java.lang.String currentFileName;

    private ST classUserRoutinesSection;	// filled directly (through visitor returns)
    private ST classInitRoutinesSection;	// filled collaterally ("global" access)

    private ST classNamesSection;
    private ST classObjectsSection;
    private ST classPrototypeObjectsSection;
    private ST classDispTablesSection;

    private ST kStringSection;
    private final HashMap<java.lang.String, Integer> kStringPool = new HashMap<>();

    private ST kIntSection;
    private final HashMap<java.lang.Integer, Integer> kIntPool = new HashMap<>();

    private ST kBoolSection;
    private final HashMap<java.lang.Boolean, Integer> kBoolPool = new HashMap<>();

    private int defineConstant(java.lang.String konstant) {
        var kId = kStringPool.putIfAbsent(konstant, kStringPool.size());
        if (kId == null) {
            kId = kStringPool.size() - 1;

            var kLenId = defineConstant(konstant.length());
            var totalObjectSize = 4 + ((konstant.length() + MIPS_WORD_SIZE) / MIPS_WORD_SIZE);

            var st = templates.getInstanceOf("konstantString")
                    .add("newStringkId", kId)
                    .add("stringTag", ActualClassSymbol.STRING.getTag())
                    .add("stringObjSize", totalObjectSize)
                    .add("intLenkId", kLenId)
                    .add("ascii", konstant);

            kStringSection.add("e", st);
        }

        return kId;
    }

    private int defineConstant(java.lang.Integer konstant) {
        var kId = kIntPool.putIfAbsent(konstant, kIntPool.size());
        if (kId == null) {
            kId = kIntPool.size() - 1;

            var st = templates.getInstanceOf("konstantInt")
                    .add("newIntkId", kId)
                    .add("intTag", ActualClassSymbol.INT.getTag())
                    .add("int", konstant);

            kIntSection.add("e", st);
        }

        return kId;
    }

    private int defineConstant(java.lang.Boolean konstant) {
        var kId = kBoolPool.putIfAbsent(konstant, kBoolPool.size());
        if (kId == null) {
            kId = kBoolPool.size() - 1;

            var st = templates.getInstanceOf("konstantBool")
                    .add("newBoolkId", kId)
                    .add("boolTag", ActualClassSymbol.BOOL.getTag())
                    .add("bool", konstant.compareTo(false));

            kBoolSection.add("e", st);
        }

        return kId;
    }

    private void assignClassTagIds(ClassSymbol sym) {
        var parentClass = (ClassSymbol) sym.getParent();

        // Determin eticheta clasei
        int parentMaxSubTreeTag = (parentClass == null) ? 0 : parentClass.getMaxSubTreeTag();
        sym.setTag(parentMaxSubTreeTag);
        sym.setMaxSubTreeTag(parentMaxSubTreeTag + 1);

        // Parcurgere recursivă a subclaselor (în ordine DFS -- important pentru asignarea corectă a tagurilor)
        for (var c : sym.getChildren()) {
            assignClassTagIds(c);
        }

        if (parentClass != null) {
            parentClass.setMaxSubTreeTag(sym.getMaxSubTreeTag());
        }
    }

    private int getAttrOffset(IdSymbol sym) {
        assert sym.getDefinitionType() == IdSymbol.DefinitionType.ATTRIBUTE;
        return (sym.getIndex() + MIPS_PROT_OBJ_HEADER_NUM_WORDS) * MIPS_WORD_SIZE;
    }

    private int getLocalOffset(IdSymbol sym) {
        assert sym.getDefinitionType() == IdSymbol.DefinitionType.LOCAL;
        return (-sym.getIndex() - MIPS_NUM_WORDS_UNTIL_FIRST_LOCAL_FROM_FP) * MIPS_WORD_SIZE;
    }

    private int getFormalOffset(IdSymbol sym) {
        assert sym.getDefinitionType() == IdSymbol.DefinitionType.FORMAL;
        return (sym.getIndex() + MIPS_NUM_WORDS_UNTIL_FIRST_FORMAL_FROM_FP) * MIPS_WORD_SIZE;
    }

    private java.lang.String getShortTypeName(ClassSymbol type) {
        if (type.isPrimitive()) {
            if (type == ActualClassSymbol.STRING)
                return "str";

            return type.toString().toLowerCase();
        }

        return null;
    }

    private ST generateDefaultValue(ClassSymbol sym) {
        var shortType = getShortTypeName(sym);
        if (shortType == null) {
            return templates.getInstanceOf("loadImm")
                    .add("imm", 0);
        }
        else {
            return templates.getInstanceOf("loadConstant")
                    .add("class", shortType)
                    .add("id", 0);
        }
    }

    private void createClassLayout(ClassSymbol sym) {
        // Trebuie parcurse clasele în ordinea dată de tag-uri. Din cauza asta nu pot folosi visitorul.
        // Ordinea obligatorie vine din faptul că tabelele claselor (nume/rutine de inițializare) sunt indexate după tag

        // Adăugare clasă în tabela de nume
        int kNameId = defineConstant(sym.getName());
        var nameTabEntry = templates.getInstanceOf("nameTabEntry")
                .add("stringkId", kNameId);
        classNamesSection.add("e", nameTabEntry);

        // Adăugare clasă în tabela de obiecte
        var objTabEntry = templates.getInstanceOf("objTabEntry")
                .add("class", sym.getName());
        classObjectsSection.add("e", objTabEntry);

        // Construiesc tabela de atribute (plus caching pentru acces facil în viitor)
        var attrTable = sym.getAttrTable();

        // Creez codul pentru obiectul prototip
        var defaultValues = attrTable.stream()
                .filter(attr -> !attr.getName().startsWith("$"))
                .map(attr -> {
                    var shortType = getShortTypeName(attr.getType().getActualType());
                    return (shortType == null) ? "0" : (shortType + "_const0");
                })
                .collect(Collectors.toList());

        var protObj = templates.getInstanceOf("protObj" + (sym.isBuiltIn() ? sym : "" ))
                .add("class", sym.getName())
                .add("tag", sym.getTag())
                .add("size", attrTable.size() + MIPS_PROT_OBJ_HEADER_NUM_WORDS)
                .add("attrs", defaultValues);
        classPrototypeObjectsSection.add("e", protObj);

        // Construiesc tabela de dispatch (plus caching pentru acces facil în viitor)
        var vmTable = sym.getVMTable();

        // Creez codul aferent tabelei de dispatch
        var dispTable = templates.getInstanceOf("dispTable").add("class", sym.getName());
        for (var m : vmTable) {
            dispTable.add("entry", m.toString());
        }
        classDispTablesSection.add("e", dispTable);

        // Creez codul aferent rutinei de inițializare (doar pentru clasele definite implicit)
        if (sym.isBuiltIn()) {
            var initRoutine = templates.getInstanceOf("initRoutine")
                    .add("class", sym.getName())
                    .add("parentClass", sym.getParent());
            classInitRoutinesSection.add("e", initRoutine);
        }

        // Parcurg în continuare clasele în ordinea dată de tag indices
        for (var c : sym.getChildren()) {
            createClassLayout(c);
        }
    }

    @Override
    public ST visit(Id id) {
        var idSymbol = id.getSymbol();

        if (idSymbol.getName().equals("self")) {
            return templates.getInstanceOf("loadSelf");
        }

        return switch (idSymbol.getDefinitionType()) {
            case ATTRIBUTE -> templates.getInstanceOf("loadAttr")
                    .add("offset", getAttrOffset(idSymbol));
            case LOCAL -> templates.getInstanceOf("loadFpRelative")
                    .add("offset", getLocalOffset(idSymbol));
            case FORMAL -> templates.getInstanceOf("loadFpRelative")
                    .add("offset", getFormalOffset(idSymbol));
            default -> throw new RuntimeException("DEBUG: shouldn't get here");
        };
    }

    @Override
    public ST visit(Int int_) {
        var val = Integer.parseInt(int_.getToken().getText());
        var kId = defineConstant(val);

        return templates.getInstanceOf("loadConstant")
                .add("class", "int")
                .add("id", kId);
    }

    @Override
    public ST visit(String string) {
        var val = string.getToken().getText();
        var kId = defineConstant(val);

        return templates.getInstanceOf("loadConstant")
                .add("class", "str")
                .add("id", kId);
    }

    @Override
    public ST visit(Bool bool_) {
        var val = Boolean.parseBoolean(bool_.getToken().getText());
        var kId = defineConstant(val);

        return templates.getInstanceOf("loadConstant")
                .add("class", "bool")
                .add("id", kId);
    }

    @Override
    public ST visit(If if_) {
        return templates.getInstanceOf("conditional")
                .add("cond", if_.cond.accept(this))
                .add("thenBranch", if_.thenBranch.accept(this))
                .add("elseBranch", if_.elseBranch.accept(this))
                .add("uniq", uniqCounter++);
    }

    @Override
    public ST visit(While while_) {
        return templates.getInstanceOf("whileLoop")
                .add("cond", while_.cond.accept(this))
                .add("body", while_.body.accept(this))
                .add("uniq", uniqCounter++);
    }

    @Override
    public ST visit(Block block) {
        var st = templates.getInstanceOf("sequence");

        block.exprs.forEach(x -> st.add("e", x.accept(this)));
        return st;
    }

    @Override
    public ST visit(LocalDef localDef) {
        return generateAssignmentCode(localDef.id, localDef.initValue, true);
    }

    @Override
    public ST visit(Let let) {
        var st = templates.getInstanceOf("sequence");

        let.vars.forEach(x -> st.add("e", x.accept(this)));
        st.add("e", let.body.accept(this));
        return st;
    }

    @Override
    public ST visit(New new_) {
        var name = new_.type.getToken().getText();

        if (name.equals("SELF_TYPE")) {
            return templates.getInstanceOf("newSELF_TYPE");
        }
        else {
            return templates.getInstanceOf("newStatic")
                    .add("class", name);
        }
    }

    @Override
    public ST visit(IsVoid isVoid) {
        return templates.getInstanceOf("isVoid")
                .add("e", isVoid.instance.accept(this))
                .add("uniq", uniqCounter++);
    }

    private ST generateAssignmentCode(Id destNode, Expression exprNode, boolean generateDefault) {
        // Verificare dacă există expresie de inițializare (eg: pentru let)
        if (exprNode == null && !generateDefault)
            return null;

        var idSymbol = destNode.getSymbol();
        var exprCode = (exprNode == null)
                ? generateDefaultValue(destNode.getSymbol().getType().getActualType())
                : exprNode.accept(this);

        return switch (idSymbol.getDefinitionType()) {
            case ATTRIBUTE -> templates.getInstanceOf("storeAttr")
                    .add("code", exprCode)
                    .add("offset", getAttrOffset(idSymbol));
            case LOCAL -> templates.getInstanceOf("storeFpRelative")
                    .add("code", exprCode)
                    .add("offset", getLocalOffset(idSymbol));
            case FORMAL -> templates.getInstanceOf("storeFpRelative")
                    .add("code", exprCode)
                    .add("offset", getFormalOffset(idSymbol));
            default -> throw new RuntimeException("DEBUG: shouldn't get here");
        };
    }

    @Override
    public ST visit(Assign assign) {
        return generateAssignmentCode(assign.id, assign.expr, false);
    }

    @Override
    public ST visit(Relational rel) {
        var operator = rel.getToken().getType();

        if (operator == CoolParser.EQUAL) {
            return templates.getInstanceOf("equalityTest")
                    .add("e1", rel.left.accept(this))
                    .add("e2", rel.right.accept(this))
                    .add("uniq", uniqCounter++);
        } else {
            var cmpInstruction = switch(operator) {
                case CoolParser.LT -> "blt";
                case CoolParser.LE -> "ble";

                default -> throw new IllegalStateException("Unexpected value: " + operator);
            };

            return templates.getInstanceOf("compare")
                    .add("e1", rel.left.accept(this))
                    .add("e2", rel.right.accept(this))
                    .add("x", cmpInstruction)
                    .add("uniq", uniqCounter++);
        }
    }

    @Override
    public ST visit(Not not) {
        return templates.getInstanceOf("not")
                .add("e", not.expr.accept(this))
                .add("uniq", uniqCounter++);
    }

    @Override
    public ST visit(Plus plus) {
        return templates.getInstanceOf("arithmeticOp")
                .add("e1", plus.left.accept(this))
                .add("e2", plus.right.accept(this))
                .add("x", "add");
    }

    @Override
    public ST visit(Minus minus) {
        return templates.getInstanceOf("arithmeticOp")
                .add("e1", minus.left.accept(this))
                .add("e2", minus.right.accept(this))
                .add("x", "sub");
    }

    @Override
    public ST visit(Mult mult) {
        return templates.getInstanceOf("arithmeticOp")
                .add("e1", mult.left.accept(this))
                .add("e2", mult.right.accept(this))
                .add("x", "mul");
    }

    @Override
    public ST visit(Div div) {
        return templates.getInstanceOf("arithmeticOp")
                .add("e1", div.left.accept(this))
                .add("e2", div.right.accept(this))
                .add("x", "div");
    }

    @Override
    public ST visit(Negate negate) {
        return templates.getInstanceOf("negateOp")
                .add("e", negate.expr.accept(this));
    }

    @Override
    public ST visit(Dispatch dispatch) {
        var st = templates.getInstanceOf("dispatch")
                .add("static", (dispatch.type == null) ? null : dispatch.type.getSymbol().getName())
                .add("offset", MIPS_WORD_SIZE * dispatch.id.getSymbol().getIndex())
                .add("uniq", uniqCounter++)
                .add("filekId", defineConstant(currentFileName))
                .add("line", dispatch.getToken().getLine());

        for (int i = dispatch.args.size() - 1; i >= 0; i--) {
            st.add("args", dispatch.args.get(i).accept(this));
        }

        if (dispatch.instance != null)
            st.add("instance", dispatch.instance.accept(this));
        else
            st.add("instance", templates.getInstanceOf("loadSelf"));

        return st;
    }

    @Override
    public ST visit(AttributeDef attributeDef) {
        return generateAssignmentCode(attributeDef.id, attributeDef.initValue, false);
    }

    @Override
    public ST visit(MethodDef methodDef) {
        MethodSymbol sym = (MethodSymbol) methodDef.id.getSymbol();
        int stackForLocals = sym.getTotalLocalDefs() * MIPS_WORD_SIZE;

        return templates.getInstanceOf("userRoutine")
                .add("name", sym.toString())
                .add("code", methodDef.body.accept(this))
                .add("locals", (stackForLocals == 0) ? null : stackForLocals)
                .add("stackFixup", (methodDef.formals.size() + MIPS_NUM_WORDS_UNTIL_FIRST_FORMAL_FROM_FP) * MIPS_WORD_SIZE);
    }

    @Override
    public ST visit(ClassDef class_) {
        var sym = class_.type.getSymbol();

        currentFileName = new File(Compiler.fileNames.get(class_.getParserRuleContext())).getName();

        // Creez codul aferent rutinei de inițializare
        var initRoutine = templates.getInstanceOf("initRoutine")
                .add("class", sym.getName())
                .add("parentClass", sym.getParent());

        // Zona în care este inserat codul generat depinde de tipul de cod
        for (var feat : class_.features) {
            var st = feat.accept(this);

            if (feat instanceof AttributeDef) {
                // Pentru atribute, codul se pune în rutina de inițializare a clasei
                initRoutine.add("initCode", st);
            }
            else {
                // Pentru metode, se pune în zona de rutine generate de utilizator
                classUserRoutinesSection.add("e", st);
            }
        }

        classInitRoutinesSection.add("e", initRoutine);
        return null;
    }

    @Override
    public ST visit(Program program) {
        kStringSection = templates.getInstanceOf("sequence");
        kIntSection = templates.getInstanceOf("sequence");
        kBoolSection = templates.getInstanceOf("sequence");

        classNamesSection = templates.getInstanceOf("sequence");
        classObjectsSection = templates.getInstanceOf("sequence");
        classPrototypeObjectsSection = templates.getInstanceOf("sequence");
        classDispTablesSection = templates.getInstanceOf("sequence");

        classInitRoutinesSection = templates.getInstanceOf("sequence");
        classUserRoutinesSection = templates.getInstanceOf("sequence");

        // Este nevoie de două treceri prin ierarhia de clase:

        // În primă etapă se asignează etichete tuturor claselor.
        assignClassTagIds(ActualClassSymbol.OBJECT);

        // Aceste constante sunt cerute explicit de runtime (ordinea contează)
        defineConstant(false);
        defineConstant(true);

        // Acestea nu sunt, dar e util să am constantele implicite (0, empty string) pe indexul 0 în kPool
        defineConstant(0);
        defineConstant("");

        createClassLayout(ActualClassSymbol.OBJECT);

        program.classes.forEach(x -> x.accept(this));

        // assembly-ing it all together. HA! get it?
        return templates.getInstanceOf("program")
                .add("tagInt", ActualClassSymbol.INT.getTag())
                .add("tagString", ActualClassSymbol.STRING.getTag())
                .add("tagBool", ActualClassSymbol.BOOL.getTag())
                .add("kStrings", kStringSection)
                .add("kInts", kIntSection)
                .add("kBools", kBoolSection)
                .add("nameTab", classNamesSection)
                .add("objTab", classObjectsSection)
                .add("objPrototypes", classPrototypeObjectsSection)
                .add("objDispTables", classDispTablesSection)
                .add("initRoutines", classInitRoutinesSection)
                .add("userRoutines", classUserRoutinesSection);
    }
}
