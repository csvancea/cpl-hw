package cool.parser.AST;

import cool.structures.ActualClassSymbol;
import cool.structures.ClassSymbol;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.HashMap;
import java.util.stream.Collectors;

public class ASTCodeGenPassVisitor extends ASTDefaultVisitor<ST> {
    private static final int MIPS_WORD_SIZE = 4;
    private static final STGroupFile templates = new STGroupFile("cgen.stg");

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
                    .add("boolTag", ActualClassSymbol.INT.getTag())
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
                    var type = attr.getType().getActualType();
                    if (type == ActualClassSymbol.INT || type == ActualClassSymbol.STRING || type == ActualClassSymbol.BOOL)
                        return type.toString().toLowerCase() + "_const" + type.getTag();

                    return "0";
                })
                .collect(Collectors.toList());

        var protObj = templates.getInstanceOf("protObj" + (sym.isBuiltIn() ? sym : "" ))
                .add("class", sym.getName())
                .add("tag", sym.getTag())
                .add("size", attrTable.size() + 3)
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
                .add("class", "string")
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
    public ST visit(AttributeDef attributeDef) {
        if (attributeDef.initValue != null)
            return attributeDef.initValue.accept(this);

        return null;
    }

    @Override
    public ST visit(MethodDef methodDef) {
        var st = templates.getInstanceOf("userRoutine")
                .add("name", methodDef.id.getSymbol().toString())
                .add("code", methodDef.body.accept(this));

        return st;
    }

    @Override
    public ST visit(ClassDef class_) {
        var sym = class_.type.getSymbol();

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
        var programST = templates.getInstanceOf("program")
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

        return programST;
    }
}
