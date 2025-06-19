public class Student extends Person {
    private String nim;

    public Student(String name, int age, String address, String nim) {
        super(name, age, address);
        this.nim = nim;
    }

    @Override
    public String getType() {
        return "Mahasiswa";
    }

    @Override
    public String getId() {
        return nim;
    }
}
