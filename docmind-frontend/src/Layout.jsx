import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function Layout() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-brand">DocMind</div>
        <nav className="app-nav">
          <NavLink to="/chat" className={({ isActive }) => (isActive ? 'nav-link nav-link-active' : 'nav-link')}>Chat</NavLink>
          <NavLink to="/documents" className={({ isActive }) => (isActive ? 'nav-link nav-link-active' : 'nav-link')}>Documents</NavLink>
        </nav>
        <div className="app-user">
          <span>{auth.username} <span className="role-tag">{auth.role}</span></span>
          <button className="logout-button" onClick={handleLogout}>Log out</button>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
