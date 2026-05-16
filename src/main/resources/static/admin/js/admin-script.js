document.addEventListener("DOMContentLoaded", function () {
    // Sidebar Toggle
    const sidebarCollapse = document.getElementById('sidebarCollapse');
    const sidebar = document.getElementById('sidebar');

    if (sidebarCollapse) {
        sidebarCollapse.addEventListener('click', function () {
            sidebar.classList.toggle('active');
        });
    }

    // Active menu highlighting based on URL
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('#sidebar ul li a');
    
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href !== '/' && currentPath.includes(href)) {
            link.parentElement.classList.add('active');
        } else if (href === '/' && currentPath === '/') {
            link.parentElement.classList.add('active');
        }
    });
});
