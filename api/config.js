export default function handler(req, res) {
  res.setHeader('Cache-Control', 's-maxage=300, stale-while-revalidate=600');
  res.status(200).json({
    supabaseUrl: process.env.SUPABASE_URL || 'https://vkveimpvrpnzelbsvdrg.supabase.co',
    supabaseAnonKey: process.env.SUPABASE_PUBLISHABLE_KEY || process.env.SUPABASE_ANON_KEY || ''
  });
}
