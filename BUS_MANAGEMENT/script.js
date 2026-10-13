function showSection(id) {
    document.getElementById(id).scrollIntoView({
        behavior: "smooth"
    });
}

function openModal() {
    document.getElementById("busModal").style.display = "flex";
}

function closeModal() {
    document.getElementById("busModal").style.display = "none";
}

document.getElementById("bookingForm").addEventListener("submit", function(event) {

    event.preventDefault();

    let passenger = document.getElementById("passenger").value;
    let from = document.getElementById("from").value;
    let to = document.getElementById("to").value;

    document.getElementById("bookingMessage").innerHTML =
        "✓ Booking confirmed for " + passenger +
        " — " + from + " to " + to;

    this.reset();
});

document.getElementById("busForm").addEventListener("submit", function(event) {

    event.preventDefault();

    let busNumber = document.getElementById("newBusNumber").value;
    let route = document.getElementById("newRoute").value;
    let driver = document.getElementById("newDriver").value;
    let capacity = document.getElementById("newCapacity").value;

    let table = document.getElementById("busTable");

    let row = table.insertRow();

    row.innerHTML = `
        <td>#NEW</td>
        <td>${busNumber}</td>
        <td>${route}</td>
        <td>${driver}</td>
        <td>${capacity}</td>
        <td><span class="status active">Active</span></td>
    `;

    this.reset();
    closeModal();
});

window.onclick = function(event) {

    let modal = document.getElementById("busModal");

    if (event.target === modal) {
        closeModal();
    }
};