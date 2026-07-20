package cz.humblej.squares.app;

final class NetworkAddress {
    private final String address;
    private final String adapterName;

    NetworkAddress(String address, String adapterName) {
        this.address = address;
        this.adapterName = adapterName;
    }

    String address() {
        return address;
    }

    @Override
    public String toString() {
        return address + " - " + adapterName;
    }
}
