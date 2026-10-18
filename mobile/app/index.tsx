import { useQuery } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator, FlatList, Pressable, RefreshControl, StyleSheet,
  Text, View, Linking,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { fetchNews } from '../src/api/news';
import type { NewsArticle } from '../src/types/news';

function formatDate(value?: string | null) {
  if (!value) return 'Publication time unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Publication time unavailable' : date.toLocaleString();
}

function ArticleCard({ article }: { article: NewsArticle }) {
  return (
    <Pressable style={styles.card} onPress={() => Linking.openURL(article.url)} accessibilityRole="link">
      <Text style={styles.source}>{article.source}{article.category ? ` · ${article.category}` : ''}</Text>
      <Text style={styles.title}>{article.title}</Text>
      {article.summary ? <Text style={styles.summary} numberOfLines={3}>{article.summary.replace(/<[^>]*>/g, ' ').trim()}</Text> : null}
      <View style={styles.cardFooter}>
        <Text style={styles.date}>{formatDate(article.publishedAt)}</Text>
        <Text style={styles.readMore}>Read original ↗</Text>
      </View>
    </Pressable>
  );
}

export default function NewsScreen() {
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);
  const news = useQuery({
    queryKey: ['news'],
    queryFn: fetchNews,
    refetchInterval: 60_000,
    refetchOnWindowFocus: true,
  });
  const refreshing = news.isFetching && !news.isLoading;

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <View>
          <Text style={styles.eyebrow}>YOUR DAILY BRIEFING</Text>
          <Text style={styles.heading}>News Reader</Text>
          <Text style={styles.subheading}>Latest available articles · VnExpress</Text>
        </View>
        <Pressable style={styles.refreshButton} onPress={() => { setLastRefresh(new Date()); void news.refetch(); }} accessibilityLabel="Refresh news">
          <Text style={styles.refreshText}>↻ Refresh</Text>
        </Pressable>
      </View>

      {news.isLoading ? (
        <View style={styles.center}><ActivityIndicator size="large" /><Text style={styles.stateText}>Loading real news…</Text></View>
      ) : news.isError ? (
        <View style={styles.stateBox}>
          <Text style={styles.stateTitle}>Could not load news</Text>
          <Text style={styles.stateText}>{news.error instanceof Error ? news.error.message : 'Unknown network error'}</Text>
          <Text style={styles.stateText}>Check the API URL and make sure the backend is running.</Text>
          <Pressable style={styles.primaryButton} onPress={() => void news.refetch()}><Text style={styles.primaryButtonText}>Try again</Text></Pressable>
        </View>
      ) : (
        <FlatList
          data={news.data?.items ?? []}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <ArticleCard article={item} />}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setLastRefresh(new Date()); void news.refetch(); }} />}
          ListEmptyComponent={<View style={styles.stateBox}><Text style={styles.stateTitle}>No articles available</Text><Text style={styles.stateText}>No feed entries have been collected yet. Try again later.</Text></View>}
          ListHeaderComponent={<Text style={styles.updated}>Showing {news.data?.count ?? 0} stored articles · API response {formatDate(news.data?.responseAt)}</Text>}
          ListFooterComponent={<Text style={styles.footer}>Source: VnExpress. Select a story to open the original article. {lastRefresh ? `Last manual refresh: ${lastRefresh.toLocaleTimeString()}` : ''}</Text>}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#F5F7FA' },
  header: { paddingHorizontal: 20, paddingTop: 18, paddingBottom: 16, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12, backgroundColor: '#FFFFFF', borderBottomWidth: 1, borderBottomColor: '#E5EAF0' },
  eyebrow: { fontSize: 10, fontWeight: '800', letterSpacing: 1.6, color: '#64748B' },
  heading: { fontSize: 28, fontWeight: '800', color: '#102A43', marginTop: 5 },
  subheading: { fontSize: 12, color: '#64748B', marginTop: 4 },
  refreshButton: { backgroundColor: '#E8F1FF', paddingVertical: 10, paddingHorizontal: 12, borderRadius: 12 },
  refreshText: { color: '#175CD3', fontWeight: '700' },
  list: { padding: 14, paddingBottom: 32 },
  updated: { color: '#64748B', fontSize: 11, marginBottom: 10, lineHeight: 16 },
  card: { backgroundColor: '#FFFFFF', borderRadius: 16, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#E7ECF2' },
  source: { color: '#175CD3', fontSize: 11, fontWeight: '800', textTransform: 'uppercase', marginBottom: 8 },
  title: { color: '#102A43', fontSize: 17, fontWeight: '700', lineHeight: 24 },
  summary: { color: '#526579', fontSize: 13, lineHeight: 19, marginTop: 8 },
  cardFooter: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 14, gap: 8 },
  date: { color: '#8291A3', fontSize: 10, flex: 1 },
  readMore: { color: '#175CD3', fontSize: 11, fontWeight: '700' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  stateBox: { padding: 28, alignItems: 'center', justifyContent: 'center', gap: 12, flexGrow: 1 },
  stateTitle: { fontSize: 18, fontWeight: '800', color: '#102A43', textAlign: 'center' },
  stateText: { color: '#64748B', fontSize: 13, lineHeight: 20, textAlign: 'center' },
  primaryButton: { backgroundColor: '#175CD3', paddingHorizontal: 18, paddingVertical: 11, borderRadius: 12, marginTop: 6 },
  primaryButtonText: { color: '#FFFFFF', fontWeight: '700' },
  footer: { color: '#8291A3', fontSize: 10, textAlign: 'center', lineHeight: 16, marginTop: 6, marginBottom: 20 },
});
