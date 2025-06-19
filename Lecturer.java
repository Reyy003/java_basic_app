public class Lecturer extends Person {
    private String nip;

    public Lecturer(String name, int age, String address, String nip) {
        super(name, age, address);
        this.nip = nip;
    }

    @Override
    public String getType() {
        return "Dosen";
    }

    @Override
    public String getId() {
        return nip;
    }
}
