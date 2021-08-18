function contentReport(){
    var contentReport__data = new URLSearchParams();
    var contentReport__userAgent = navigator.userAgent;
    var contentReport__problemBox = document.getElementById("contentReport__problemBox").value;
    var contentReport__reportForm = document.getElementById("contentReport__form");
    var contentReport__error =document.getElementById("contentReport__error");
    var contentReport__success = document.getElementById("contentReport__success");

    contentReport__data.append("site", document.getElementById("contentReport__site").value);
    contentReport__data.append("title", document.getElementById("contentReport__title").value);
    contentReport__data.append("problemBox", contentReport__problemBox);
    contentReport__data.append("directUrl", document.getElementById("contentReport__directUrl").value);
    contentReport__data.append("userTime", document.getElementById("contentReport__userTime").value);
    contentReport__data.append("userTimeZone", document.getElementById("contentReport__userTimeZone").value);
    contentReport__data.append("serverLabel", document.getElementById("contentReport__serverLabel").value);
    contentReport__data.append("userAgent", contentReport__userAgent);

    fetch("/direct/portal-chat/content", {
        method: 'post',
        body: contentReport__data
    })
        .then(function (response) {
            return response.text();
        })
        .then(function (text) {
            console.log(text);
            contentReport__reportForm.style.display = 'none';
            if (text != 'SUCCESS'){
                contentReport__error.style.display= 'block';
            } else{
                contentReport__success.style.display= 'block';
            }
            document.getElementById('contentReport__close').focus();
        })
        .catch(function (error) {
            console.log(error)
        });
    return false;
}